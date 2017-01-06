# SKOSifier

Quick and dirty tool for converting CSV file to SKOS (RDF)

The first line of the CSV file must contain the following column headers:
  - "ID": unique ID, used to generate the URI
  - "parent": parent ID of this term (empty if there is no parent)
  - language tag (e.g. "nl", "fr".... one column per language)
