import psycopg

conn = psycopg.connect(
    host='172.31.0.3',
    port=5432,
    dbname='interview',
    user='testuser',
    password='test123'
)
print('Connected with psycopg3')

cur = conn.cursor()
cur.execute("SELECT COUNT(*) FROM interview_experience")
count = cur.fetchone()
print(f'Current count: {count[0]}')

conn.close()
