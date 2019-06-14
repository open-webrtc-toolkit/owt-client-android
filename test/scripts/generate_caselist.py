import argparse
import json
import os
import re
import sys

HOME_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
TEST_PATH = os.path.join(HOME_PATH, 'test')
BASE_CASES = os.path.join(TEST_PATH, 'base/src/main/java/owt/test/base')
CONF_CASES = os.path.join(TEST_PATH,
                          'conference/apiTest/src/main/java/owt/test/conference/apitest')
P2P_CASES = os.path.join(TEST_PATH, 'p2p/apiTest/src/main/java/owt/test/p2p/apitest')

CASE_PATTERN = ".*?public void (test.*?)\(\)"


def gen_case(module_name, case_path):
    print('\n> generating case list for module', module_name)
    case_data = {'module': module_name, 'cases': []}
    case_regex = re.compile(CASE_PATTERN)

    for file in os.listdir(case_path):
        with open(os.path.join(case_path, file), 'r') as case_file:
            for line in case_file:
                result = case_regex.match(line)
                if result:
                    case_data['cases'].append(file.split('.')[0] + "#" + result.group(1))
    print('> done,', len(case_data['cases']), 'cases in total.')
    return case_data


def output_caselist(cast_list, output_file):
    print('\n> writing case list to a file')
    with open(output_file, 'w') as file:
        json.dump(case_list, file)
    print('> done. Please find me here:', output_file, '\n')


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Generate a list of test cases used by run_test.py '
                    'and output this list to the file designated by --output.'
                    'Testcase list will be a json formatted object as:'
                    '{[{\'module\': <module>, \'cases\': [<case>]}]}')

    parser.add_argument('--base', default=False, dest='base', action='store_true',
                        help='Generate case list for base module.')
    parser.add_argument('--conference', default=False, dest='conference', action='store_true',
                        help='Generate case list for conference module.')
    parser.add_argument('--p2p', default=False, dest='p2p', action='store_true',
                        help='Generate case list for p2p module.')
    parser.add_argument('--output', default=os.path.join(TEST_PATH, 'case_list.json'),
                        dest='output', help='Output file location.')
    args = parser.parse_args()

    # generate case lists
    case_list = []
    if args.base:
        case_list.append(gen_case('base', BASE_CASES))
    if args.p2p:
        case_list.append(gen_case('p2p', P2P_CASES))
    if args.conference:
        case_list.append(gen_case('conference', CONF_CASES))

    if len(case_list) == 0:
        print('No cases found, you need to at least designate one module to generate cases.\n')
        sys.exit(0)
    # output case list to a file
    output_caselist(case_list, args.output)
