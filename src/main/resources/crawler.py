import sys
import json
import re
from urllib.parse import quote
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from selenium.common.exceptions import TimeoutException, NoSuchElementException

def get_text(element, selector, default="정보 없음"):
    try:
        return element.find_element(By.CSS_SELECTOR, selector).text
    except:
        return default

def main(keywords):
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

    options = webdriver.ChromeOptions()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-gpu")
    options.add_argument("--log-level=3")
    options.page_load_strategy = 'eager' 

    try:
        service = Service(ChromeDriverManager().install())
        driver = webdriver.Chrome(service=service, options=options)
    except Exception as e:
        # 드라이버 초기화 실패 시 빈 리스트 반환
        print("[]")
        return

    all_results = []

    try:
        base_url = "https://map.kakao.com/?q="

        # 단일 키워드 검색(프론트 검색 화면)인 경우 상위 N개 반환
        # 여러 키워드 배치(AI 등)인 경우 키워드당 1개(속도) 유지
        max_items_per_keyword = 10 if len(keywords) == 1 else 1
        
        for keyword in keywords:
            try:
                encoded_keyword = quote(keyword)
                driver.get(base_url + encoded_keyword)
                
                # 검색 결과 리스트 대기 (최대 3초)
                try:
                    # PC 버전 선택자
                    places_container = WebDriverWait(driver, 3).until(
                        EC.presence_of_element_located((By.ID, "info.search.place.list"))
                    )

                    places = places_container.find_elements(By.CSS_SELECTOR, "li.PlaceItem")
                    if not places:
                        raise NoSuchElementException("No PlaceItem")

                    for place in places[:max_items_per_keyword]:
                        # 데이터 추출
                        name = get_text(place, 'a.link_name', keyword)
                        category = get_text(place, 'span.subcategory', "기타")
                        
                        # 주소 추출 (여러 선택자 시도)
                        address = get_text(place, 'div.addr > p', "")
                        if not address:
                            address = get_text(place, 'p[data-id="address"]', "정보 없음")

                        rating = get_text(place, 'span.score > em.num', "0.0")

                        # 링크 (가능하면 항목 링크, 아니면 검색 링크)
                        link = ""
                        try:
                            href = place.find_element(By.CSS_SELECTOR, 'a.link_name').get_attribute('href')
                            if href and href.startswith('http'):
                                link = href
                        except:
                            link = ""
                        if not link:
                            link = base_url + encoded_keyword
                        
                        # 이미지
                        try:
                            image_url = place.find_element(By.CSS_SELECTOR, 'div.photo_area a.link_photo').get_attribute('style')
                            # style="background-image: url('...')" 형태에서 URL 추출
                            match = re.search(r'url\("?(.+?)"?\)', image_url)
                            if match:
                                image_url = match.group(1)
                            else:
                                image_url = ""
                        except:
                            image_url = ""

                        all_results.append({
                            "storeName": name,
                            "category": category,
                            "address": address,
                            "rating": rating,
                            "reviewCount": "0", # 리뷰 수는 생략하거나 별도 파싱 필요
                            "link": link,
                            "imageUrl": image_url
                        })

                except (TimeoutException, NoSuchElementException):
                    # 검색 결과가 없는 경우 더미 데이터 추가
                    all_results.append({
                        "storeName": keyword,
                        "category": "기타",
                        "address": "정보 없음",
                        "rating": "0.0",
                        "reviewCount": "0",
                        "link": base_url + quote(keyword),
                        "imageUrl": ""
                    })

            except Exception:
                # 개별 키워드 처리 중 에러 발생 시 무시하고 계속 진행
                continue

    finally:
        driver.quit()
        print(json.dumps(all_results, ensure_ascii=False))

if __name__ == "__main__":
    # 첫 번째 인자는 스크립트 경로이므로 제외하고 나머지 인자들을 키워드로 사용
    if len(sys.argv) > 1:
        keywords = [arg.strip('"').strip("'") for arg in sys.argv[1:] if arg is not None]
        keywords = [k.strip() for k in keywords if k.strip()]
        if keywords:
            main(keywords)
        else:
            print("[]")
    else:
        print("[]")