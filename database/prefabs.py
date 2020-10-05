# encoding: utf-8
"""
prefabs

Python Library for interacting with mongoDB prefabs collection.

"""
import urllib.parse
import pprint
import sys
import os
import json
import re
import ujson
#import pyyaml

from pymongo import MongoClient
from bson.json_util import loads, dumps, RELAXED_JSON_OPTIONS, CANONICAL_JSON_OPTIONS

#mongodb_opendc_db = os.environ['OPENDC_DB']
#mongodb_opendc_user = os.environ['OPENDC_DB_USERNAME']
#mongodb_opendc_password = os.environ['OPENDC_DB_PASSWORD']

#if mongodb_opendc_db == None or mongodb_opendc_user == None or mongodb_opendc_password == None:
#	print("One or more environment variables are not set correctly. \nYou may experience issues connecting to the mongodb database.")

user = urllib.parse.quote_plus('opendc') #TODO: replace this with environment variable
password = urllib.parse.quote_plus('opendcpassword') #TODO: same as above
database = urllib.parse.quote_plus('opendc')

client = MongoClient('mongodb://%s:%s@localhost/default_db?authSource=%s' % (user, password, database))
opendcdb = client.opendc
prefabs_collection = opendcdb.prefabs


def add(prefab_file, name):
	if(re.match(r"\w+(\\\ \w*)*\.json", prefab_file)):
		try:
			with open(prefab_file, "r") as json_file:
				json_prefab = json.load(json_file)
				#print(json_prefab)
				if name != None:
					json_prefab["name"] = name
				try:
					prefab_id = prefabs_collection.insert(json_prefab)
				except ConnectionFailure:
					print("ERROR: Could not connect to the mongoDB database.")
				except DuplicateKeyError:
					print("ERROR: A prefab with the same unique ID already exists in the database. \nPlease remove the '_id' before trying again.\nYour prefab has not been imported.")
				except:
					print("ERROR: A general error has occurred. Your prefab has not been imported.")
				if prefab_id != None:
					if name != None:
						print(f'Prefab "{name}" has been imported successfully.')
					else:
						print(f'Prefab "{prefab_file}" has been imported successfully.')
		except FileNotFoundError:
			print(f"ERROR: {prefab_file} could not be found in the specified path. No prefabs have been imported.")
	elif(re.match(r"\w+(\\\ \w*)*\.yml", prefab_file)):
		print("expecting a yaml file here")
		#yaml
	else:
		print("The filetype provided is an unsupported filetype.")
		#unsupported filetype

def clone(prefab_name, new_name):
	bson = prefabs_collection.find_one({'name': prefab_name})
	json_string = dumps(bson) #convert BSON representation to JSON
	chosen_prefab = json.loads(json_string) #load as a JSON object

	chosen_prefab.pop("_id") # clean out our _id field from the export: mongo will generate a new one if this is imported back in

	if new_name != None:
			chosen_prefab["name"] = new_name
	try:
		prefab_id = prefabs_collection.insert_one(chosen_prefab)
	except ConnectionFailure:
		print("ERROR: Could not connect to the mongoDB database.")
	except:
		print("ERROR: A general error has occurred. Your selected prefab has not been cloned.")
	if prefab_id != None:
		if new_name != None:
			print(f'Prefab "{prefab_name}" has been cloned successfully as {new_name}.')
		else:
			print(f'Prefab "{prefab_name}" has been cloned successfully.')

def export(prefab_name, type):
	bson = prefabs_collection.find_one({'name': prefab_name})
	json_string = dumps(bson) #convert BSON representation to JSON
	chosen_prefab = json.loads(json_string) #load as a JSON object

	chosen_prefab.pop("_id") # clean out our _id field from the export: mongo will generate a new one if this is imported back in

	with open(f'{prefab_name}.json', 'w', encoding='utf8') as f:
		json.dump(chosen_prefab, f, ensure_ascii=False, indent=4)
	print(f'Prefab {prefab_name} written to {os.getcwd()}/{prefab_name}.json.')
	#pprint.pprint(json_string)
	#pprint.pprint(json.loads(str(json_string)))

def list():
	#TODO: why does it output in single quotations?
	cursor = prefabs_collection.find()
	prefabs = []
	for record in cursor:
		#pprint.pprint(record)
		#print(record)
		json_string = dumps(record, json_options=RELAXED_JSON_OPTIONS) ##pymongo retrieves BSON objects, which need to be converted to json for pythons json module
		prefabs.append(json.loads(json_string))

	#print(f'There are {str(len(prefabs))} prefabs in the database. They are:')
	print("Name 			Author")
	for prefab in prefabs:
		if(prefab['visibility'] == "private"):
			continue
		print(f"{prefab['name']} 		{prefab['author']}")
		#pprint.pprint(prefab)


def remove(prefab_name):
	prefabs_collection.delete_one({'name': prefab_name})


