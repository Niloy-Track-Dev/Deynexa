#!/bin/bash
find . -type f -name "*.kt" -o -name "*.kts" -o -name "*.xml" -o -name "*.md" | while read -r file; do
  sed -i 's/com.example/com.niloy/g' "$file"
done

mkdir -p app/src/main/java/com/niloy
if [ -d "app/src/main/java/com/example" ]; then
    cp -r app/src/main/java/com/example/* app/src/main/java/com/niloy/
    rm -rf app/src/main/java/com/example
fi
