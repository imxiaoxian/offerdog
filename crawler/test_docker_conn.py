import psycopg2
import sys

try:
    conn = psycopg2.connect(
        host='postgres',
        port=5432,
        database='interview',
        user='postgres',
        password='postgres',
        sslmode='disable'
    )
    print('Connected successfully!')
    
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM interview_experience")
    count = cur.fetchone()
    print(f'Current interview_experience count: {count[0]}')
    
    cur.close()
    conn.close()
except Exception as e:
    print(f'Error: {e}')
    sys.exit(1)
