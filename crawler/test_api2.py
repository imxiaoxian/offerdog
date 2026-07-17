import requests

url = 'https://www.nowcoder.com/api/feed/mainFeed?type=1&query=%E7%99%BE%E5%BA%A6&page=1&pageSize=20'
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'application/json, text/plain, */*',
    'Referer': 'https://www.nowcoder.com/',
}

resp = requests.get(url, headers=headers, timeout=10)
print(f'Status: {resp.status_code}')
print(f'Content-Type: {resp.headers.get("Content-Type")}')
print(f'Response: {resp.text[:1000]}')
