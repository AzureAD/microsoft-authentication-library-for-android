import sys
import xml.etree.ElementTree as ET

def get_coverage(xml_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()
    # Get the overall counter (direct child of report element, not nested in packages/classes)
    counter = root.find("./counter[@type='INSTRUCTION']")
    covered = int(counter.attrib['covered'])
    missed = int(counter.attrib['missed'])
    return covered / (covered + missed)

def main():
    pr_cov = get_coverage(sys.argv[1])
    dev_cov = get_coverage(sys.argv[2])

    print(f"PR branch coverage: {pr_cov:.2%}")
    print(f"Dev branch coverage: {dev_cov:.2%}")

    if pr_cov < dev_cov:
        print("ERROR: PR branch coverage is lower than dev branch. Failing...")
        sys.exit(1)
    else:
        print("SUCCESS: PR branch coverage is not lower than dev branch, this is acceptable!")
        sys.exit(0)

main()