import requests
import json

def send_message_to_user(access_token, user_id, message):
    url = "https://api.line.me/v2/bot/message/push"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {access_token}"
    }
    data = {
        "to": user_id,
        "messages": [
            {"type": "text", "text": message}
        ]
    }
    
    response = requests.post(url, headers=headers, json=data)

def get_line_uid():
    with open("config.json") as f:
        data = json.load(f)
    return data["lineUid"],data["lineToken"]


