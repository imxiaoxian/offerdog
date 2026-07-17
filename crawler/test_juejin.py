import requests
import json
import re

session = requests.Session()
session.headers.update({
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'application/json',
})

# 尝试掘金搜索API
keywords = ["面经", "面试经", "BAT面试"]

for kw in keywords:
    print(f"\n=== Search: {kw} ===")
    url = f"https://api.juejin.cn/content_api/v1/search/article?search_key={kw}&page=1&size=10"
    try:
        response = session.get(url, timeout=10)
        if response.status_code == 200:
            data = response.json()
            print(f"Response: {data}")
    except Exception as e:
        print(f"Error: {e}")

# 尝试热门文章API
print("\n=== Hot Articles ===")
url = "https://api.juejin.cn/content_api/v1/article/hot/list?page=1&size=10"
try:
    response = session.get(url, timeout=10)
    if response.status_code == 200:
        data = response.json()
        if data.get('err_no') == 0:
            articles = data.get('data', [])
            print(f"Found {len(articles)} articles")
            for art in articles[:5]:
                if 'article_info' in art:
                    info = art['article_info']
                    print(f"  - {info.get('title', 'N/A')[:60]}")
except Exception as e:
    print(f"Error: {e}")
