#!/usr/bin/env python3
#Change shebang to /usr/bin/python3 before using with docker
# encoding: utf-8
"""
prefab

CLI frontend for viewing, modifying and creating prefabs in OpenDC.

"""
import sys
import prefabs

def usage():
	print("Usage: prefab add <prefab>:      imports a prefab from JSON")
	print("	      list:              lists all (public) prefabs")
	print("	      export <prefab> [json|yaml]:   exports the specified prefab to the specified filetype (with JSON used by default)")
	print("	      clone <prefab> [new prefab name]:   clones the specified prefab, giving the new prefab a name if specified")
	print("	      remove <prefab>:   removes the specified prefab from the database")

def interactive(): #interactive CLI mode: recommended
	print("OpenDC Prefab CLI")
	running = True
	while(exit):
		print(">", end=" ")
		try:
			command = input()
			command = command.split()
		except EOFError as e:
			print("exit")
			print("bye!")
			exit()
		except KeyboardInterrupt as KI:
			print("\nbye!")
			exit()
		if(len(command) >= 1):
			if(command[0] == "exit"):
				print("bye!")
				exit()
			elif(command[0] == "list"): # decrypt
				prefabs.list()
			elif(command[0] == "help"): # decrypt
				usage()
			elif(command[0] == "add"):
				if(len(command) == 3):
					prefabs.add(command[1], command[2])
				else:
					prefabs.add(command[1], None)
			elif(command[0] == "clone"):
				if(len(command) == 3):
					prefabs.clone(command[1], command[2])
				else:
					prefabs.clone(command[1], None)
			elif(command[0] == "export"):
				#print(sys.argv[2])
				prefabs.export(command[1], "json")
			elif(command[0] == "remove"):
				print("WARNING: Doing so will permanently remove the specified prefab. \nThis action CANNOT be undone. Please type the name of the prefab to confirm deletion.")
				confirm = input()
				if confirm == command[1]:
					prefabs.remove(command[1])
					print(f'Prefab {command[1]} has been removed.')
				else:
					print("Confirmation failed. The prefab has not been removed.")
			else:
				print("prefabs: try 'help' for more information\n")
		else:
			print("prefabs: try 'help' for more information\n")


def main():
	if(len(sys.argv) >= 2):
		if(sys.argv[1] == "list"): # decrypt
			prefabs.list()
			exit()
		#elif(sys.argv[1] == "-e"): # encrypt
		#	encrypt(sys.argv[2], sys.argv[3], sys.argv[4])
		#elif(sys.argv[1] == "-v"): # verify
		#	verify(sys.argv[2], sys.argv[3], sys.argv[4])
		elif(sys.argv[1] == "help"): # decrypt
			usage()
			exit()
		elif(sys.argv[1] == "add"):
			if(sys.argv[3]):
				prefabs.add(sys.argv[2], sys.argv[3])
			else:
				prefabs.add(sys.argv[2])
			exit()
		elif(sys.argv[1] == "export"):
			#print(sys.argv[2])
			prefabs.export(sys.argv[2], "json")
			exit()
		elif(sys.argv[1] == "remove"):
			print("WARNING: Doing so will permanently remove the specified prefab. \nThis action CANNOT be undone. Please type the name of the prefab to confirm deletion.")
			confirm = input()
			if confirm == sys.argv[2]:
				prefabs.remove(sys.argv[2])
				print(f'Prefab {sys.argv[2]} has been removed.')
			else:
				print("Confirmation failed. The prefab has not been removed.")
			exit()
		else:
			print("prefabs: try 'prefabs help' for more information\n")
	elif(len(sys.argv) == 1):
		interactive()

	else:
	#	print "Incorrect number of arguments!\n"
		print("prefabs: try 'prefabs help' for more information\n")


if __name__ == "__main__":
	main()
