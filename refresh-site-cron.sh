######
# This file has to be manually copied to the server
#####

# safer bash script
set -o nounset -o errexit -o pipefail
# don't split on spaces, only on lines
IFS=$'\n\t'

readonly TARGET_DIR="$1"
readonly NEXUS_ROOT="http://nexus.usethesource.io/content/repositories/snapshots/org/rascalmpl/docs-tutor-site/1.0.0-SNAPSHOT"

curl -s -f -L "$NEXUS_ROOT/maven-metadata.xml" > /tmp/docs-manifest.xml

readonly last_script_version=$( sed -n 's/^[ \t]*<value>\([^<]*\)<.*$/\1/p' /tmp/docs-manifest.xml | head -n 1 )
readonly current_version=$(cat "$1/version" )

if [ "$last_script_version" == "$current_version" ]; then
    exit 0
else
    echo "updating: $last_script_version"
    readonly SCRATCH=$(mktemp -d -t "sync-docs-XXXXX")
    finish() {
        rm -rf "$SCRATCH"
    }
    trap finish EXIT

   cd "$SCRATCH"
    if ! curl -f -s -L "$NEXUS_ROOT/docs-tutor-site-$last_script_version.tar.gz" | tar zx; then
        echo "Error downloading new docs site"
        exit 1
    fi

    echo `ls``
    mv "site/" "$TARGET_DIR/site-$last_script_version"
    if ! ln -fns "$TARGET_DIR/site-$last_script_version/www" "$TARGET_DIR/www"; then
        echo "Could not move to the new version"
        exit 1
    fi

    readonly two_versions_ago = $(cat "$1/old_version")
    echo "$last_script_version" > "$1/version"
    echo "$current_version" > "$1/old_version"

    rm -rf "$TARGET_DIR/site-$two_versions_ago" || echo "Could not cleanup old version"

    echo "Migrated to new version, and cleaned-up old"
    exit 1
fi