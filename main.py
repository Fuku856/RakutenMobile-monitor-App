from line import send_message_to_user, get_line_uid
from rakuten import module_main
import sys
UID,TOKEN = get_line_uid()

mode=sys.argv
mode=mode[1]
send_message_to_user(TOKEN, UID, module_main(mode))