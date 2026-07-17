import requests
import json

# 尝试不同的API端点
urls_to_try = [
    'https://www.nowcoder.com/api/feed/mainFeed?type=1&query=%E7%99%BE%E5%BA%A6&page=1&pageSize=20',
    'https://gw-c.nowcoder.com/api/feed/mainFeed?type=1&query=%E7%99%BE%E5%BA%A6&page=1&pageSize=20',
    'https://www.nowcoder.com/search/all?query=%E7%99%BE%E5%BA%A6&type=feed&page=1',
]

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'application/json',
    'Referer': 'https://www.nowcoder.com/',
}

for url in urls_to_try:
    try:
        resp = requests.get(url, headers=headers, timeout=10)
        print(f'URL: {url}')
        print(f'Status: {resp.status_code}')
        if resp.status_code == 200 and 'json' in resp.headers.get('Content-Type', ''):
            print(f'Content-Type: {resp.headers.get("Content-Type")}')
            print(f'Response: {resp.text[:500]}')
        print('---')
    except Exception as e:
        print(f'Error: {e}')
        print('---')
