#!/bin/bash

# Function to print help
print_help() {
    echo "Usage: ./clone-vehicle.sh [OPTIONS]"
    echo ""
    echo "Clones a Neptus vehicle definition file (.nvcl) and updates specific parameters."
    echo ""
    echo "Options:"
    echo "  --source <path>      Path to source file. Default: vehicles-defs/09-autonaut-01.nvcl"
    echo "  --id <string>        (Required) All lowercase vehicle name, no spaces (e.g., autonaut-02)"
    echo "  --name <string>      (Required) Full name, mix case and spaces allowed (e.g., Autonaut 02)"
    echo "  --imei <string>      (Required) Primary IMEI number"
    echo "  --imei1 <string>     (Optional) Secondary IMEI number"
    echo "  --ip <string>        (Required) IP address of the vehicle"
    echo "  --imcid <string>     (Required) IMC id in hex format (e.g., 08:06)"
    echo "  --icon-color <rgb>   (Optional) RGB icon color (e.g., '255,51,153')"
    echo "  --dest <path>        (Optional) Path for the output file. Defaults to creating a new file alongside the source."
    echo "  -h, --help           Show this help message"
    echo ""
    echo "Example:"
    echo "  ./clone-vehicle.sh --source vehicles-defs/09-autonaut-01.nvcl \\"
    echo "                     --id autonaut-02 \\"
    echo "                     --name \"Autonaut 02\" \\"
    echo "                     --imei 300534068640999 \\"
    echo "                     --ip 10.1.0.2 \\"
    echo "                     --imcid 08:06 \\"
    echo "                     --icon-color \"255,0,0\""
}

# Dependency check
if ! command -v xmlstarlet &> /dev/null; then
    echo "Error: 'xmlstarlet' is not installed."
    echo "Please install it to continue (e.g., 'sudo apt update && sudo apt install xmlstarlet')."
    exit 1
fi

# Default values
SOURCE_FILE="vehicles-defs/09-autonaut-01.nvcl"
ID=""
NAME=""
IMEI=""
IMEI1=""
IP=""
IMCID=""
ICON_COLOR=""
DEST_FILE=""

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --source) SOURCE_FILE="$2"; shift ;;
        --id) ID="$2"; shift ;;
        --name) NAME="$2"; shift ;;
        --imei) IMEI="$2"; shift ;;
        --imei1) IMEI1="$2"; shift ;;
        --ip) IP="$2"; shift ;;
        --imcid) IMCID="$2"; shift ;;
        --icon-color) ICON_COLOR="$2"; shift ;;
        --dest) DEST_FILE="$2"; shift ;;
        -h|--help) print_help; exit 0 ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

# 1. Check if mandatory fields are provided
if [[ -z "$ID" || -z "$NAME" || -z "$IMEI" || -z "$IP" || -z "$IMCID" ]]; then
    echo "Error: Missing required arguments."
    echo ""
    print_help
    exit 1
fi

# 2. Validations using regex
if [[ ! "$ID" =~ ^[a-z0-9-]+$ ]]; then
    echo "Error: --id must be lowercase and contain no spaces (e.g., autonaut-02)"
    exit 1
fi

if [[ ! "$IMCID" =~ ^[0-9a-fA-F]{2}:[0-9a-fA-F]{2}$ ]]; then
    echo "Error: --imcid must be in hex format dd:dd (e.g., 08:06)"
    exit 1
fi

if [[ -n "$ICON_COLOR" && ! "$ICON_COLOR" =~ ^[[:space:]]*[0-9]{1,3}[[:space:]]*,[[:space:]]*[0-9]{1,3}[[:space:]]*,[[:space:]]*[0-9]{1,3}[[:space:]]*$ ]]; then
    echo "Error: --icon-color must be an RGB string (e.g., '255,51,153')"
    exit 1
fi

# 3. Resolve source path and check if it exists
if [[ ! -f "$SOURCE_FILE" ]]; then
    echo "Error: Source file not found at '$SOURCE_FILE'"
    exit 1
fi

# 4. Determine Destination File if not provided
if [[ -z "$DEST_FILE" ]]; then
    PARENT_DIR=$(dirname "$SOURCE_FILE")
    EXTENSION="${SOURCE_FILE##*.}"
    DEST_FILE="$PARENT_DIR/$ID.$EXTENSION"
fi

echo -e "\e[36mReading source file: $SOURCE_FILE\e[0m"

# Copy source to destination to work on it in-place
cp "$SOURCE_FILE" "$DEST_FILE"

# 5. Modify the required fields using xmlstarlet

# Update primary fields
xmlstarlet ed -L \
    -u "/system/properties/id" -v "$ID" \
    -u "/system/properties/name" -v "$NAME" \
    -u "/system/communication-means/comm-mean/host-address" -v "$IP" \
    -u "/system/communication-means/comm-mean/protocols-args/imc/imc-id" -v "$IMCID" \
    -u "/system/protocols-supported/protocols-args/iridium/imei" -v "$IMEI" \
    "$DEST_FILE"

# -> properties > appearance > icon-color (Optional)
if [[ -n "$ICON_COLOR" ]]; then
    # Split color string by comma and remove spaces
    IFS=',' read -r r g b <<< "$ICON_COLOR"
    r=$(echo "$r" | xargs); g=$(echo "$g" | xargs); b=$(echo "$b" | xargs)
    
    xmlstarlet ed -L \
        -u "/system/properties/appearance/icon-color/r" -v "$r" \
        -u "/system/properties/appearance/icon-color/g" -v "$g" \
        -u "/system/properties/appearance/icon-color/b" -v "$b" \
        "$DEST_FILE"
fi

# -> protocols-supported > protocols-args > iridium > imei1
if [[ -n "$IMEI1" ]]; then
    # Check if imei1 node exists
    if xmlstarlet sel -Q -t -c "/system/protocols-supported/protocols-args/iridium/imei1" "$DEST_FILE"; then
        # Exists, update it
        xmlstarlet ed -L -u "/system/protocols-supported/protocols-args/iridium/imei1" -v "$IMEI1" "$DEST_FILE"
    else
        # Doesn't exist, create it inside <iridium>
        xmlstarlet ed -L -s "/system/protocols-supported/protocols-args/iridium" -t elem -n "imei1" -v "$IMEI1" "$DEST_FILE"
    fi
else
    # If not provided, ensure it is removed
    xmlstarlet ed -L -d "/system/protocols-supported/protocols-args/iridium/imei1" "$DEST_FILE"
fi

# 6. Formatting completion
echo -e "\e[32mSuccessfully cloned vehicle definition to: $DEST_FILE\e[0m"
