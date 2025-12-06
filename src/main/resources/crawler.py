import sys
import json
import re
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
        
        for keyword in keywords:
            try:
                driver.get(base_url + keyword)
                
                # 검색 결과 리스트 대기 (최대 3초)
                try:
                    # PC 버전 선택자
                    places_container = WebDriverWait(driver, 3).until(
                        EC.presence_of_element_located((By.ID, "info.search.place.list"))
                    )
                    # 첫 번째 결과만 가져옴 (속도 최적화)
                    place = places_container.find_element(By.CSS_SELECTOR, "li.PlaceItem")
                    
                    # 데이터 추출
                    name = get_text(place, 'a.link_name', keyword)
                    category = get_text(place, 'span.subcategory', "기타")
                    
                    # 주소 추출 (여러 선택자 시도)
                    address = get_text(place, 'div.addr > p', "")
                    if not address:
                        address = get_text(place, 'p[data-id="address"]', "정보 없음")

                    rating = get_text(place, 'span.score > em.num', "0.0")
                    
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
                        "link": "",
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
                        "link": "",
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
        keyword = sys.argv[1].strip('"').strip("'")
        main(keyword)
    else:
        print("[]")