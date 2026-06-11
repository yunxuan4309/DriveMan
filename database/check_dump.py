#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Robust SQL dump consistency checker with proper SQL value parsing."""
import re
import sys

sys.stdout.reconfigure(encoding='utf-8')

with open('D:/java/DriveMan/database/dump_all_data_utf8.sql', 'r', encoding='utf-8') as f:
    content = f.read()

# ============================================================
# Step 1: Count rows per table
# ============================================================
table_row_counts = {}
inserts = re.findall(r"INSERT INTO `(\w+)`", content)
for tn in set(inserts):
    pattern = re.escape(f"INSERT INTO `{tn}`") + r".*?VALUES\s*(.*?);"
    matches = re.findall(pattern, content, re.DOTALL)
    total = 0
    for m in matches:
        rows = m.count('),(') + 1
        total += rows
    table_row_counts[tn] = total

print('=== 各表数据量 ===')
for tn in sorted(table_row_counts.keys()):
    print(f'  {tn}: {table_row_counts[tn]}')

# ============================================================
# Step 2: Robust SQL value parser
# ============================================================
def parse_sql_values(values_text):
    """
    Parse SQL VALUES clause into a list of rows, each row is a list of values.
    Handles: quoted strings with commas, NULL, nested parens in JSON, etc.
    """
    text = values_text.strip()
    rows = []
    i = 0
    while i < len(text):
        if text[i] == '(':
            # Start of a row
            depth = 1
            j = i + 1
            while j < len(text) and depth > 0:
                if text[j] == "'":
                    # Skip quoted string
                    j += 1
                    while j < len(text):
                        if text[j] == '\\':
                            j += 2
                            continue
                        if text[j] == "'":
                            break
                        j += 1
                elif text[j] == '(':
                    depth += 1
                elif text[j] == ')':
                    depth -= 1
                j += 1
            row_text = text[i+1:j-1]
            rows.append(parse_sql_row(row_text))
            i = j
        elif text[i] == ',':
            i += 1
        elif text[i] in ' \t\n\r':
            i += 1
        else:
            i += 1
    return rows

def parse_sql_row(row_text):
    """
    Parse a single row of SQL values into individual values.
    Properly handles quoted strings with embedded commas.
    """
    values = []
    i = 0
    current = []
    while i < len(row_text):
        ch = row_text[i]
        if ch == "'":
            # Start of quoted string
            j = i + 1
            while j < len(row_text):
                if row_text[j] == '\\':
                    j += 2
                    continue
                if row_text[j] == "'":
                    break
                j += 1
            current.append(row_text[i:j+1])  # include quotes
            i = j + 1
        elif ch == ',':
            # End of value
            values.append(''.join(current).strip())
            current = []
            i += 1
        elif ch == '(' or ch == ')':
            # Nested parens (JSON, etc.) - include as-is
            current.append(ch)
            i += 1
        else:
            current.append(ch)
            i += 1
    if current:
        values.append(''.join(current).strip())
    return values

def extract_ids_from_table(table_name, id_column, content):
    """Extract all id_column values from a specific table's INSERT using robust parser."""
    # Find the INSERT statement for this table
    pattern = re.escape(f"INSERT INTO `{table_name}`") + r"\s*\(([^)]+)\)\s*VALUES\s*([\s\S]*?);"
    m = re.search(pattern, content)
    if not m:
        return set()

    cols_str = m.group(1)
    values_str = m.group(2)

    cols = [c.strip().strip('`') for c in cols_str.split(',')]
    try:
        id_idx = cols.index(id_column)
    except ValueError:
        return set()

    rows = parse_sql_values(values_str)
    ids = set()
    for row in rows:
        if id_idx < len(row):
            val = row[id_idx].strip("'").strip()
            if val not in ('NULL', '', 'null'):
                ids.add(val)
    return ids

# ============================================================
# Step 3: Extract key reference sets
# ============================================================
uids = extract_ids_from_table('user', 'user_id', content)
coach_ids_set = extract_ids_from_table('coach', 'coach_id', content)
coach_user_ids = extract_ids_from_table('coach', 'user_id', content)
vehicle_ids_set = extract_ids_from_table('vehicle', 'id', content)
venue_ids_set = extract_ids_from_table('venue', 'id', content)
es_ids = extract_ids_from_table('exam_session', 'id', content)

print(f'\n=== 基础数据 ===')
print(f'user_id 总数: {len(uids)}')
print(f'coach_id 总数: {len(coach_ids_set)}')
print(f'coach.user_id 总数: {len(coach_user_ids)}')
print(f'vehicle_id 总数: {len(vehicle_ids_set)}')
print(f'venue_id 总数: {len(venue_ids_set)}')
print(f'exam_session_id 总数: {len(es_ids)}')

# Show user IDs sorted numerically
uid_list = sorted(uids, key=lambda x: int(x) if x.isdigit() else 999999)
print(f'\nuser_id 列表: {", ".join(uid_list)}')

# Check for gaps: find all user_ids with role=2 (coach) to see coach mapping
# Extract user table data more thoroughly to check role
def extract_user_data(content):
    """Extract user_id + role + username + real_name from user table."""
    pattern = re.escape("INSERT INTO `user`") + r"\s*\(([^)]+)\)\s*VALUES\s*([\s\S]*?);"
    m = re.search(pattern, content)
    if not m:
        return []
    cols_str = m.group(1)
    values_str = m.group(2)
    cols = [c.strip().strip('`') for c in cols_str.split(',')]

    try:
        uid_idx = cols.index('user_id')
        role_idx = cols.index('role')
        uname_idx = cols.index('username')
        realname_idx = cols.index('real_name')
    except ValueError:
        return []

    rows = parse_sql_values(values_str)
    result = []
    for row in rows:
        uid = row[uid_idx].strip("'") if uid_idx < len(row) else ''
        role = row[role_idx].strip("'") if role_idx < len(row) else ''
        uname = row[uname_idx].strip("'") if uname_idx < len(row) else ''
        rname = row[realname_idx].strip("'") if realname_idx < len(row) else ''
        result.append((uid, role, uname, rname))
    return result

users = extract_user_data(content)
role1_ids = set(u[0] for u in users if u[1] == '1')
role2_ids = set(u[0] for u in users if u[1] == '2')
role3_ids = set(u[0] for u in users if u[1] == '3')

print(f'\n用户角色分布:')
print(f'  学员(role=1): {len(role1_ids)} 人')
print(f'  教练(role=2): {len(role2_ids)} 人')
print(f'  管理员(role=3): {len(role3_ids)} 人')

# Show full user table for reference
print(f'\n用户列表:')
print(f'  {"ID":>4} {"Role":>4} {"Username":<20} {"RealName":<10}')
print(f'  {"-"*42}')
for uid, role, uname, rname in sorted(users, key=lambda x: int(x[0]) if x[0].isdigit() else 9999):
    print(f'  {uid:>4} {role:>4} {uname:<20} {rname:<10}')

# ============================================================
# Step 4: Reference checking
# ============================================================
issues = []
checked = 0

def check_fk(table, col, values, ref_set, label):
    global checked
    for v in values:
        checked += 1
        if v not in ref_set and v != 'NULL':
            issues.append(f'{table}.{col}={v} ({label})')

# Appointment: student_id -> user, coach_id -> user
appt_ids_student = extract_ids_from_table('appointment', 'student_id', content)
appt_ids_coach = extract_ids_from_table('appointment', 'coach_id', content)
check_fk('appointment', 'student_id', appt_ids_student, uids, '学员不存在')
check_fk('appointment', 'coach_id', appt_ids_coach, uids, '教练不存在')

# Training record
tr_ids_student = extract_ids_from_table('training_record', 'student_id', content)
tr_ids_coach = extract_ids_from_table('training_record', 'coach_id', content)
check_fk('training_record', 'student_id', tr_ids_student, uids, '学员不存在')
check_fk('training_record', 'coach_id', tr_ids_coach, uids, '教练不存在')

# Exam registration
er_ids_student = extract_ids_from_table('exam_registration', 'student_id', content)
er_ids_session = extract_ids_from_table('exam_registration', 'session_id', content)
check_fk('exam_registration', 'student_id', er_ids_student, uids, '学员不存在')
check_fk('exam_registration', 'session_id', er_ids_session, es_ids, '场次不存在')

# Payment record
pr_ids = extract_ids_from_table('payment_record', 'student_id', content)
check_fk('payment_record', 'student_id', pr_ids, uids, '学员不存在')

# Familiarization record
fr_ids = extract_ids_from_table('familiarization_record', 'student_id', content)
fr_sess = extract_ids_from_table('familiarization_record', 'exam_session_id', content)
check_fk('familiarization_record', 'student_id', fr_ids, uids, '学员不存在')
check_fk('familiarization_record', 'exam_session_id', fr_sess, es_ids, '场次不存在')

# Retake training
rt_ids = extract_ids_from_table('retake_training_record', 'student_id', content)
check_fk('retake_training_record', 'student_id', rt_ids, uids, '学员不存在')

# Physical exam
pe_ids = extract_ids_from_table('physical_exam', 'student_id', content)
check_fk('physical_exam', 'student_id', pe_ids, uids, '学员不存在')

# License upgrade
lu_ids = extract_ids_from_table('license_upgrade', 'student_id', content)
check_fk('license_upgrade', 'student_id', lu_ids, uids, '学员不存在')

# Special exam
se_ids = extract_ids_from_table('special_exam_record', 'student_id', content)
check_fk('special_exam_record', 'student_id', se_ids, uids, '学员不存在')

# Disability info
di_ids = extract_ids_from_table('disability_info', 'user_id', content)
check_fk('disability_info', 'user_id', di_ids, uids, '用户不存在')

# Special person
sp_ids = extract_ids_from_table('special_person_record', 'user_id', content)
check_fk('special_person_record', 'user_id', sp_ids, uids, '用户不存在')

# File
f_ids = extract_ids_from_table('file', 'user_id', content)
check_fk('file', 'user_id', f_ids, uids, '用户不存在')

# Coach user mapping: coach.user_id -> user
check_fk('coach', 'user_id', coach_user_ids, uids, '教练在user表中无对应')

# Coach application
ca_ids_student = extract_ids_from_table('coach_application', 'student_id', content)
ca_ids_coach = extract_ids_from_table('coach_application', 'coach_id', content)
ca_ids_src = extract_ids_from_table('coach_application', 'source_coach_id', content)
check_fk('coach_application', 'student_id', ca_ids_student, uids, '学员不存在')

# Coach vehicle application
cva_ids = extract_ids_from_table('coach_vehicle_application', 'coach_id', content)
check_fk('coach_vehicle_application', 'coach_id', cva_ids, coach_ids_set, '教练ID不存在')

# Coach schedule
cs_coach = extract_ids_from_table('coach_schedule', 'coach_id', content)
cs_vehicle = extract_ids_from_table('coach_schedule', 'vehicle_id', content)
cs_venue = extract_ids_from_table('coach_schedule', 'venue_id', content)
check_fk('coach_schedule', 'coach_id', cs_coach, coach_ids_set, '教练ID不存在')
check_fk('coach_schedule', 'vehicle_id', cs_vehicle, vehicle_ids_set, '车辆不存在')
check_fk('coach_schedule', 'venue_id', cs_venue, venue_ids_set, '场地不存在')

# Student-coach
sc_student = extract_ids_from_table('student_coach', 'student_id', content)
sc_coach = extract_ids_from_table('student_coach', 'coach_id', content)
check_fk('student_coach', 'student_id', sc_student, uids, '学员不存在')
check_fk('student_coach', 'coach_id', sc_coach, coach_ids_set, '教练ID不存在')

# ============================================================
# Step 5: Business logic checks
# ============================================================

# Check: coach references role=2 users only
print(f'\n=== 业务逻辑检查 ===')

# Which role=2 users have coach records?
coach_uid_set = coach_user_ids
role2_with_coach = role2_ids & coach_uid_set
role2_without_coach = role2_ids - coach_uid_set

print(f'教练角色用户(role=2)中有教练记录: {len(role2_with_coach)} 人')
if role2_without_coach:
    for uid in sorted(role2_without_coach, key=int):
        uname = next((u[2] for u in users if u[0] == uid), '?')
        issues.append(f'user.user_id={uid} ({uname}) 角色为教练但无 coach 记录')

# Which coach records reference non-role2 users?
for cid in coach_user_ids:
    if cid not in role2_ids:
        uname = next((u[3] for u in users if u[0] == cid), '?')
        issues.append(f'coach.user_id={cid} ({uname}) 不是role=2的用户')

# Student_coach.coach_id should exist in coach table
# Already checked above

# Check for student_coach student_id references non-role1 users
for sid in sc_student:
    if sid not in role1_ids:
        issues.append(f'student_coach.student_id={sid} 不是role=1的用户')

# ============================================================
# Results
# ============================================================
print(f'\n=== 结果: 共 {len(issues)} 个问题 ===')
if issues:
    for i, iss in enumerate(issues, 1):
        print(f'  {i}. {iss}')
else:
    print('  ✓ 未发现引用一致性问题')

print(f'\n=== 检查的引用数: {checked} ===')
print('=== 检查完成 ===')
