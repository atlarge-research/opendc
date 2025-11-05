# %%

import nbformat
from nbconvert import MarkdownExporter
from nbconvert.preprocessors import ExecutePreprocessor
from tabulate import tabulate
import base64
from bs4 import BeautifulSoup
import os
import shutil
import glob

def create_output_dir(input_dir, output_dir):

    # Make output_dir
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
    os.makedirs(output_dir, exist_ok=True)

    # Copy experiments
    if os.path.exists(f"{input_dir}/experiments"):
        shutil.copytree(f"{input_dir}/experiments", os.path.join(output_dir, "experiments"), dirs_exist_ok=True)

    # Copy topologies
    if os.path.exists(f"{input_dir}/topologies"):
        shutil.copytree(f"{input_dir}/topologies", os.path.join(output_dir, "topologies"), dirs_exist_ok=True)

    # Copy topologies
    if os.path.exists(f"{input_dir}/figures"):
        shutil.copytree(f"{input_dir}/figures", os.path.join(output_dir, "figures"), dirs_exist_ok=True)


# %%

def html_table_to_markdown(html):
    soup = BeautifulSoup(html, 'html.parser')
    table = soup.find('table')
    if not table:
        return None

    # Extract headers
    headers = [th.get_text(strip=True) for th in table.find_all('th')]

    # Extract rows
    rows = []
    for tr in table.find_all('tr')[1:]:  # Skip header row
        cells = tr.find_all(['td', 'th'])
        rows.append([cell.get_text(strip=True) for cell in cells])

    return tabulate(rows, headers=headers or [], tablefmt='pipe') + '\n'


def convert_notebook(notebook_path, output_dir):

    notebook_name = os.path.basename(notebook_path)

    output_img_dir = output_dir + "/figures/" + os.path.basename(notebook_name).replace(".ipynb", "").replace(" ", "_")

    if not os.path.exists(output_img_dir):
        os.makedirs(output_img_dir, exist_ok=True)

    output_md_path = os.path.join(output_dir, os.path.splitext(notebook_name)[0] + ".md")

    # --- Load and execute notebook ---
    with open(notebook_path, "r", encoding="utf-8") as f:
        nb = nbformat.read(f, as_version=4)

    # --- Convert output cells ---
    md_lines = []

    is_streaming = False
    for cell in nb.cells:
        if cell.cell_type == "markdown":
            md_lines.append(cell.source)
            md_lines.append("")  # extra newline

        elif cell.cell_type == "code":
            md_lines.append('<div className="code-cell">\n')
            md_lines.append('###### Input')
            # Code block
            md_lines.append("```python")
            md_lines.append(cell.source)
            md_lines.append("```")

            md_lines.append("---")

            md_lines.append("###### Output")

            # Handle outputs
            for output in cell.get("outputs", []):

                if output.output_type == 'execute_result':
                    if is_streaming:
                        is_streaming = False
                        md_lines.append('</div>\n')

                    data = output.get("data", {})

                    if 'text/html' in data:
                        md_table = html_table_to_markdown(data['text/html'])
                        if md_table:
                            md_lines.append(md_table)
                            continue  # Skip default text output

                    # Fallback: plain text
                    if "text/plain" in data:
                        md_lines.append("```")
                        md_lines.append(data["text/plain"])
                        md_lines.append("```")



                elif output.output_type == 'display_data':
                    data = output.get("data", {})
                    # Handle image output
                    if "image/png" in data:
                        img_data = base64.b64decode(data["image/png"])
                        img_filename = f"figure_{len(md_lines)}.png"
                        img_path = os.path.join(output_img_dir, img_filename)
                        with open(img_path, "wb") as img_file:
                            img_file.write(img_data)
                        md_lines.append(f"![Figure]({img_path.replace(output_dir + '/', '')})")
                        md_lines.append("")

                elif output.output_type == 'stream':
                    print("Stream output")
                    if not is_streaming:
                        is_streaming = True
                        md_lines.append('<div className="stream-output">\n')

                    md_lines.append(output["text"])

            if is_streaming:
                is_streaming = False
                md_lines.append('</div>\n')
            md_lines.append('</div>\n')

    # --- Save Markdown ---
    with open(output_md_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md_lines))

    print(f"Markdown export complete: {output_md_path}")

# %%
# --- Config ---
input_dir = "1. Simple Experiment"
output_dir = input_dir + "-markdown"

create_output_dir(input_dir, output_dir)

# --- Find notebooks ---
notebooks = glob.glob(f"{input_dir}/*.ipynb")

for notebook in notebooks:
    convert_notebook(notebook, output_dir)
