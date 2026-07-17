import requests
import re

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9',
    'Accept-Encoding': 'gzip, deflate, br',
    'Cache-Control': 'no-cache',
    'Pragma': 'no-cache',
    'Sec-Ch-Ua': '"Not(A:Brand";v="24", "Chromium";v="122"',
    'Sec-Ch-Ua-Mobile': '?0',
    'Sec-Ch-Ua-Platform': '"Windows"',
    'Sec-Fetch-Dest': 'document',
    'Sec-Fetch-Mode': 'navigate',
    'Sec-Fetch-Site': 'none',
    'Sec-Fetch-User': '?1',
    'Upgrade-Insecure-Requests': '1',
}

session = requests.Session()
session.headers.update(HEADERS)

# 先访问首页获取cookies
print("=== Getting cookies ===")
home_url = "https://www.nowcoder.com"
try:
    response = session.get(home_url, timeout=15)
    print(f"Home status: {response.status_code}")
    print(f"Cookies: {dict(session.cookies)}")
    
    # 再访问搜索页
    print("\n=== Searching ===")
    search_url = "https://www.nowcoder.com/search/all?query=阿里面经&type=all&searchType=%E9%A1%B6%E9%83%A8%E5%AF%BC%E8%88%AA%E6%A0%8F&page=1"
    response = session.get(search_url, timeout=15)
    print(f"Search status: {response.status_code}")
    
    text = response.text
    
    # 查找 feed ID
    id_patterns = [
        r'/feed/(\d+)',
        r'"feedId":\s*(\d+)',
    ]
    
    all_ids = []
    for p in id_patterns:
        matches = re.findall(p, text)
        all_ids.extend(matches)
    
    unique_ids = list(set(all_ids))
    print(f"Found {len(unique_ids)} IDs: {unique_ids[:10]}")
    
    # 如果找到 ID，尝试访问详情页
    if unique_ids:
        feed_id = unique_ids[0]
        feed_url = f"https://www.nowcoder.com/feed/{feed_id}"
        
        print(f"\n=== Trying feed {feed_id} ===")
        feed_response = session.get(feed_url, timeout=15)
        print(f"Status: {feed_response.status_code}")
        
        if '找不到了' not in feed_response.text and feed_response.status_code == 200:
            # 保存有效页面
            with open('valid_feed.html', 'w', encoding='utf-8') as f:
                f.write(feed_response.text)
            print("Saved valid feed page!")
            
            # 提取内容
            title = re.search(r'<title>([^<]+)</title>', feed_response.text)
            if title:
                print(f"Title: {title.group(1)}")
        else:
            print("Feed is invalid")
            
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
