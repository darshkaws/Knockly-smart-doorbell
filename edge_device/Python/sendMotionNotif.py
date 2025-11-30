import requests
import os

base_url = "https://mutual-osprey-actually.ngrok-free.app"
endpoint = "/notifications/notify/motionDetected"

tokenLocation = "/home/admin/.doorbell_id_token.txt"

def getToken():
	if os.path.exists(tokenLocation):
		with open(tokenLocation, 'r') as file:
			token = file.read().strip()
			return token 
	else:
		raise FileNotFoundError(f"Token not found at {tokenLocation}")
		
def personAtDoorNotif(): # call this to send notification
	token = getToken()
	url = base_url + endpoint
	data = {"token": token}
	response = requests.post(url, json=data)
	
	if response.status_code != 200:
		print("Request to send notification failed with code" + str(response.status_code))
		print(response.reason)
	

if __name__=="__main__":
    personAtDoorNotif()
