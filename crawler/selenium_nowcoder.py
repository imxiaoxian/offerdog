from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.edge.options import Options
import time
import re

edge_options = Options()
edge_options.add_argument('--headless')
edge_options.add_argument('--disable-gpu')
edge_options.add_argument('--no-sandbox')
edge_options.add_argument('--disable-dev-shm-usage')
edge_options.add_argument('--window-size=1920,1080')
edge_options.add_argument('--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
edge_options.add_argument('--accept-lang=zh-CN,zh;q=0.9')

driver = None

try:
    print("Starting Edge...")
    driver = webdriver.Edge(options=edge_options)
    
    # 访问首页
    print("Visiting homepage...")
    driver.get("https://www.nowcoder.com")
    time.sleep(3)
    print(f"Page title: {driver.title}")
    
    # 访问搜索页
    print("\nSearching for 面经...")
    search_url = "https://www.nowcoder.com/search/all?query=阿里面经&type=all&searchType=%E9%A1%B6%E9%83%A8%E5%AF%BC%E8%88%AA%E6%A0%8F&page=1"
    driver.get(search_url)
    time.sleep(5)
    
    # 获取页面源码
    page_source = driver.page_source
    print(f"Page source length: {len(page_source)}")
    
    # 查找 feed 相关内容
    feed_patterns = [
        r'/feed/(\d+)',
        r'"feedId":\s*(\d+)',
    ]
    
    all_ids = []
    for p in feed_patterns:
        matches = re.findall(p, page_source)
        all_ids.extend(matches)
    
    unique_ids = list(set(all_ids))
    print(f"Found {len(unique_ids)} feed IDs: {unique_ids[:10]}")
    
    if unique_ids:
        feed_id = unique_ids[0]
        feed_url = f"https://www.nowcoder.com/feed/{feed_id}"
        
        print(f"\n=== Visiting feed {feed_id} ===")
        driver.get(feed_url)
        time.sleep(5)
        
        title_elem = driver.title
        print(f"Title: {title_elem}")
        
        if '找不到了' not in title_elem:
            print("Feed is valid!")
            
            # 尝试提取内容
            content_selectors = [
                '.feed-detail-content',
                '.post-content',
                '.article-content',
                '[class*="content"]',
            ]
            
            for selector in content_selectors:
                try:
                    elements = driver.find_elements(By.CSS_SELECTOR, selector)
                    if elements:
                        content = elements[0].text
                        if len(content) > 100:
                            print(f"Found content ({len(content)} chars): {content[:300]}...")
                            break
                except:
                    continue
            
            with open('edge_feed.html', 'w', encoding='utf-8') as f:
                f.write(driver.page_source)
            print("Saved to edge_feed.html")
        else:
            print("Feed is invalid")
    
    print("\n=== Done ===")

except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()
    
finally:
    if driver:
        driver.quit()
