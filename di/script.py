import json

def process_json_file(file_path):
    with open(file_path, 'r') as f:
        data = json.load(f)

    # Parse the escaped JSON string in eventPayload
    event_payload_str = data['eventPayload']
    event_payload_obj = json.loads(event_payload_str)

    # Replace the string with the parsed object
    data['eventPayload'] = event_payload_obj

    # Write back to the file
    with open(file_path, 'w') as f:
        json.dump(data, f, indent=4)

# Usage example
process_json_file('9.json')
