import requests
import re

# 尝试其他技术社区
sites_to_try = [
    ("掘金", "https://api.juejin.cn/content_api/v1/article/list?tag_id=6809657767482203137&page=1&size=10"),
    ("知乎", "https://api.zhihu.com/topstory/feed-latest?limit=10"),
]

session = requests.Session()
session.headers.update({
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'application/json',
})

for name, url in sites_to_try:
    print(f"\n=== Trying {name} ===")
    try:
        response = session.get(url, timeout=10)
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"Data keys: {data.keys() if isinstance(data, dict) else type(data)}")
    except Exception as e:
        print(f"Error: {e}")
