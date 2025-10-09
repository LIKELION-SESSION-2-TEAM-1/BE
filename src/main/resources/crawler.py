import csv
import random
import re
import sys
import json
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from time import sleep

def get_element_text_or_default(parent_element, selector, default="정보 없음"):
    try:
        return parent_element.find_element(By.CSS_SELECTOR, selector).text
    except NoSuchElementException:
        return default

def main(search_keyword):
    # Python의 표준 입출력 인코딩을 UTF-8로 강제 설정
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

    options = webdriver.ChromeOptions()
    # --- [핵심] service 객체를 올바르게 생성합니다. ---
    service = Service(ChromeDriverManager().install())

    options.add_argument('user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)')
    options.add_argument("--start-maximized")
    options.add_argument("--headless")
    options.add_argument("--disable-gpu")
    options.add_argument("--log-level=3") # 불필요한 로그 메시지 숨기기

    # --- [핵심] 오타를 수정합니다: service=Service (X) -> service=service (O) ---
    driver = webdriver.Chrome(service=service, options=options)

    results = []
    try:
        base_url = "https://map.kakao.com/?q="
        driver.get(base_url + search_keyword)

        page_num = 1
        while True:
            print(f"\n--- {page_num} 페이지 크롤링 시작 ---", file=sys.stderr)

            try:
                places_container = WebDriverWait(driver, 15).until(
                    EC.presence_of_element_located((By.ID, "info.search.place.list"))
                )
                places = places_container.find_elements(By.XPATH, "./li")
            except TimeoutException:
                print("타임아웃: 가게 목록을 불러오는 데 실패했습니다.", file=sys.stderr)
                break

            for p in places:
                try:
                    detail_button = p.find_element(By.CSS_SELECTOR, 'a.moreview')
                    link = detail_button.get_attribute('href')

                    name = p.find_element(By.CSS_SELECTOR, 'a.link_name').text
                    category = get_element_text_or_default(p, 'span.subcategory', "카테고리 없음")
                    address = get_element_text_or_default(p, 'p[data-id="address"]', "주소 없음")
                    rating = get_element_text_or_default(p, 'span.score > em.num', "0")

                    review_text = get_element_text_or_default(p, 'a.review', "0")
                    review_match = re.search(r'\d+', review_text.replace(',', ''))
                    review_count = review_match.group() if review_match else "0"

                    store_data = {
                        "storeName": name,
                        "category": category,
                        "address": address,
                        "rating": rating,
                        "reviewCount": review_count,
                        "link": link
                    }
                    results.append(store_data)

                except NoSuchElementException:
                    continue

            try:
                next_btn = WebDriverWait(driver, 5).until(
                    EC.presence_of_element_located((By.ID, "info.search.page.next"))
                )
                if 'disabled' in next_btn.get_attribute('class'):
                    print("마지막 페이지에 도달했습니다.", file=sys.stderr)
                    break
                driver.execute_script("arguments[0].click();", next_btn)
                page_num += 1
                sleep(random.uniform(2, 4))
            except TimeoutException:
                print("다음 페이지 버튼을 찾지 못해 크롤링을 종료합니다.", file=sys.stderr)
                break

    finally:
        driver.quit()
        print(json.dumps(results, ensure_ascii=False))
        print(f"\n크롤링 완료! 총 {len(results)}개의 결과 출력.", file=sys.stderr)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        keyword = sys.argv[1]
        main(keyword)
    else:
        print("에러: 검색어를 입력해주세요. 예: python crawler.py 강남역맛집", file=sys.stderr)
        sys.exit(1)