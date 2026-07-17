import psycopg

try:
    conn = psycopg.connect(
        host='localhost',
        port=5432,
        dbname='interview',
        user='postgres',
        password='postgres',
        sslmode='disable',
        connect_timeout=10
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
