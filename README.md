# SKOSifier

Quick and dirty tool for converting CSV file to SKOS (RDF)

The first line of the CSV file must contain the following column headers:
  - "ID": unique ID, used to generate the URI
  - "parent": parent ID of this term (empty if there is no parent)
  - language tag (e.g. "nl", "fr".... one column per language)

In addition, the following optional headers can be used:
  - "alt_LANG" (e.g. "alt_nl") for skos:altLabel
  - "scope_LANG" (e.g. "scope_nl") for skos:scopeNote
  - "def_LANG" (e.g. "def_nl"...) for skos:definition
  - "start" for schema:startDate
  - "end" for schema:endDate

