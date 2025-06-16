.PHONY: install uninstall clean
SRCS = $(wildcard models/*) $(wildcard util/*) m3sa main.py

install: $(SRCS) venv

venv: requirements.txt
	python -m venv venv
	. venv/bin/activate && pip install -r requirements.txt
	ln -s $(PWD)/m3sa ${HOME}/.local/bin/m3sa
	@echo "WARNING: M3SA is installed under $(PWD). Do not move the directory."

uninstall:
	rm -rf venv
	rm -f ${HOME}/.local/bin/m3sa

clean:
	rm -rf __pycache__ .mypy_cache .pytest_cache
