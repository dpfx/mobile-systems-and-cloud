#!/usr/bin/env bash
# Builds the Task 1 submission PDF from the condensed docs in this folder plus two docs that are
# used unchanged (the spec, which is the prompt, and the AI log, which already fits on one page).
#
# Always builds documentation.pdf: the copy committed to the repo, title page without personal data.
# If title.local.yaml exists (gitignored: name, matriculation number, surname), it also builds
# aufgabe_1_<surname>.pdf: the copy handed in on Ilias, with that title page and otherwise identical.
#
# Usage:  submission/build.sh        Needs: pandoc, weasyprint, pdfinfo (poppler-utils).

set -euo pipefail
cd "$(dirname "$0")"
DOCS=../docs
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# Parts in the order of the PDF: section id and source file.
PARTS=(
    "environment environment.md"
    "specification $DOCS/specification.md"
    "amendments spec-amendments.md"
    "review review.md"
    "measurements measurements.md"
    "screenshots screenshots.md"
    "assessment assessment.md"
    "ai-log $DOCS/ai-log.md"
)

# One Markdown file in which every part is a div with its own id, so the stylesheet can start each
# part on a new page and style single parts (e.g. fit the spec on one page).
for part in "${PARTS[@]}"; do
    read -r id file <<< "$part"
    printf '::: {#%s .part}\n' "$id"
    cat "$file"
    printf '\n:::\n\n'
done > "$TMP/all.md"

# build <output PDF> <metadata file>...: later metadata files override earlier ones.
build() {
    local out=$1 meta=()
    shift
    for file in "$@"; do meta+=(--metadata-file "$file"); done
    # --columns is set so high that pandoc never fixes table column widths from the dashes in the
    # Markdown; WeasyPrint then sizes each column by its content.
    pandoc "$TMP/all.md" --standalone --template template.html "${meta[@]}" --columns 10000 \
        --metadata date="$(date +%Y-%m-%d)" -o "$TMP/all.html"
    # Relative paths (stylesheet, screenshots) are resolved from this folder.
    weasyprint --base-url "$PWD/" "$TMP/all.html" "$out"
    echo "Built $out: $(pdfinfo "$out" | awk '/^Pages:/ {print $2}') pages"
}

build documentation.pdf metadata.yaml

if [ -f title.local.yaml ]; then
    surname=$(sed -n 's/^surname: *//p' title.local.yaml)
    [ -n "$surname" ] || { echo "title.local.yaml has no 'surname:' line" >&2; exit 1; }
    build "aufgabe_1_${surname}.pdf" metadata.yaml title.local.yaml
else
    echo "No title.local.yaml: the hand-in copy was not built."
fi
