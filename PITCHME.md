# SKOSifier


> Converting CSV files to SKOS

---

## What is SKOS ?

- [Simple Knowledge Organization System](https://www.w3.org/2004/02/skos/)
- RDF-based W3C Recommendation
- Describing taxonomies, thesauri, code lists

+++

## Features

- Terms can have labels in multiple languages
- Preferred, alternative labels
- Hierarchy: broader, narrower terms
- Mapping =/= taxonomies: close, exact match

+++

## Example

```xml
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .

<http://vocab.belgif.be/auth/mybe-theme/FINA#id> a skos:Concept;
    skos:notation "FINA" ;
    skos:prefLabel "Financiën"@nl, "Finance"@fr ;
    skos:broadMatch <http://publications.europa.eu/resource/authority/data-theme/ECON> .
```

+++

## Benefits

- Simple to create and use
- Used by EU Publication Office, UN FAO, ...
- Possible to link to other thesauri

---

### Why this tool

- Easy conversion of existing files
- No need to install complex tools

---

## How it works

- Existing flat CSV/Excel is read
- Tool checks "known" column headers
  - Parent, languages... 
- SKOS triples are generated

---

## Limitation

- CSV files must use specific structure
- Limited to basic hierarchy
  - Should be enough in many cases

---

## Thank you

Questions ? 

@fa[twitter] @BartHanssens

@fa[envelope] [opendata@belgium.be](mailto:opendata@belgium.be)