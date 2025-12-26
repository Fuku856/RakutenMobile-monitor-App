from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
import json
import time
import calendar
import datetime
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


def module_main(mode):
    date_year = datetime.datetime.now().year
    date_month = datetime.datetime.now().month
    date_day = datetime.datetime.now().day
    date = datetime.datetime.now()
    options = Options()
    options.binary_location = "/usr/bin/google-chrome"
    options.add_argument("--headless")
    driver = webdriver.Chrome(options=options)
    mail, password = get_login()
    driver.get(
        "https://grp03.id.rakuten.co.jp/rms/nid/login?service_id=rm001&client_id=rmn_app_web&redirect_uri=https%3A%2F%2Fportal.mobile.rakuten.co.jp%2Fdashboard&scope=memberinfo_read_safebulk%2Cmemberinfo_read_point%2Cmemberinfo_get_card_token%2C30days%40Access%2C90days%40Refresh&contact_info_required=false&rae_service_id=rm001"
    )
    set_input_value(
        driver,
        "/html/body/div[2]/div/div/div[1]/div/form/div/table/tbody/tr[1]/td[2]/input",
        mail,
    )
    set_input_value(
        driver,
        "/html/body/div[2]/div/div/div[1]/div/form/div/table/tbody/tr[2]/td[2]/input",
        password,
    )
    click_button(driver, "/html/body/div[2]/div/div/div[1]/div/form/div/p[1]/input")
    time.sleep(10)
    element = WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.XPATH, '//div[@class="title__data"]'))
    )
    usage_amount = element.text
    driver.quit()
    usage_amount = format_data(usage_amount)
    message = get_message(date_year, date_month, date_day, usage_amount, mode)
    return message


def get_login():
    with open("config.json") as f:
        data = json.load(f)
    return data["mail"], data["password"]


def set_input_value(driver, xpath, value):
    input = driver.find_element(By.XPATH, xpath)
    input.send_keys(value)


def click_button(driver, xpath):
    button = driver.find_element(By.XPATH, xpath)
    button.click()


def format_data(data):
    return data.replace("GB", "").replace(" ", "")


def get_message(date_year, date_month, date_day, usage_amount, mode):
    usage_amount = float(usage_amount)  # 今日まで使った量
    month_day = calendar.monthrange(date_year, date_month)[1]  # 今月が何日あるか
    one_day_usage = 20 / int(month_day)  # 一日あたり使える量
    target_value = one_day_usage * date_day  # 今日までの使える量
    remaining_amount = 20 - usage_amount
    if mode == "nomal":
        res_text = f"今月利用できる残量は {round(remaining_amount, 2)} GB です。\n"
        available_today = target_value - usage_amount
        if available_today < 0:
           res_text += f"今日は利用を控えてください。\n{round(abs(available_today), 2)} GB 超過しています。\n"
        else:
            res_text += f"今日使える量は {round(available_today, 2)} GB です。\n"
        res_text+=f"使用量 : {usage_amount}GB/20GB"
        return res_text
    elif mode == "over":
        if target_value < usage_amount:
            return f"使用量が目標値を超えています。\n{round(abs(available_today), 2)}GB 超過しています。"
        else:
            exit()
            
    