import requests
import re

url = 'https://www.nowcoder.com/search/all?query=%E7%99%BE%E5%BA%A6&type=all&searchType=%E9%A1%B6%E9%83%A8%E5%AF%BC%E8%88%AA%E6%A0%8F&page=1'
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
}
resp = requests.get(url, headers=headers, timeout=10)

patterns = [
    r'<a[^>]+href="(https://www\.nowcoder\.com/feed/[^"]+)"',
    r'href="(/feed/\d+)"',
    r'/feed/(\d+)',
    r'"(https://www\.nowcoder\.com/feed/\d+)"',
]

for p in patterns:
    matches = re.findall(p, resp.text)
    print(f'Pattern {p}: {len(matches)} matches')
    if matches:
        print(matches[:5])
