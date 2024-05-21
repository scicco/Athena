Feature: Delta bundle between two version

  Background:
#   TODO DEV  The loging should be implment propely in tests
    #This 3 vocabulary is cached
    When user import vocabulary from the "vocabulary_20200511" schema
    And user import vocabulary from the "vocabulary_20200513" schema
    And user import vocabulary from the "vocabulary_20200515" schema
    #This vocabulary is not cached
    And user import vocabulary from the "vocabulary_20200509" schema


  Scenario: Download delta bundle
    When user inspects list of vocabulary release version
    Then it is a list containing:
      | id       | cachedDate              |
      | 20200509 |                         |
      | 20200511 | ~([0-9-]{10}T[0-9:.]+Z) |
      | 20200513 | ~([0-9-]{10}T[0-9:.]+Z) |
      | 20200515 | ~([0-9-]{10}T[0-9:.]+Z) |

    When user generates delta bundle for versions: 20200515 and 20200509
    Then it is a list containing:
      | name                 | ext | rows | path            |
      | CONCEPT_ANCESTOR     | csv | 161  | ~(.+)           |
      | CONCEPT_CLASS        | csv | 4    | ~(.+)           |
      | CONCEPT              | csv | 3    | ~(.+)           |
      | CONCEPT_RELATIONSHIP | csv | 24   | ~(.+)           |
      | CONCEPT_SYNONYM      | csv | 7    | ~(.+)           |
      | DOMAIN               | csv | 3    | ~(.+)           |
      | DRUG_STRENGTH        | csv | 3    | ~(.+)           |
      | RELATIONSHIP         | csv | 6    | ~(.+)           |
      | delta                | sql | 221  | ~(?<sqlPath>.+) |
      | VOCABULARY           | csv | 4    | ~(.+)           |
    When user compare and inspect schemas "vocabulary_20200509" and "vocabulary_20200515"
    Then it is a list containing:
      | name                 | amount1 | amount2 | missing1 | missing2 |
      | concept              | 184     | 184     | 2        | 2        |
      | concept_ancestor     | 319     | 319     | 157      | 157      |
      | concept_class        | 390     | 390     | 3        | 3        |
      | domain               | 47      | 47      | 2        | 2        |
      | concept_relationship | 664     | 668     | 14       | 10       |
      | relationship         | 10      | 10      | 4        | 4        |
      | concept_synonym      | 25      | 24      | 3        | 4        |
      | vocabulary           | 103     | 103     | 3        | 3        |
      | drug_strength        | 5       | 5       | 2        | 2        |
    When run "sqlPath" script on "vocabulary_20200509" schema
    And user compare and inspect schemas "vocabulary_20200509" and "vocabulary_20200515"
    Then it is a list containing:
      | name                 | amount1 | amount2 | missing1 | missing2 |
      | concept              | 184     | 184     | 0        | 0        |
      | concept_ancestor     | 319     | 319     | 0        | 0        |
      | concept_class        | 390     | 390     | 0        | 0        |
      | domain               | 47      | 47      | 0        | 0        |
      | concept_relationship | 668     | 668     | 0        | 0        |
      | relationship         | 10      | 10      | 0        | 0        |
      | concept_synonym      | 24      | 24      | 0        | 0        |
      | vocabulary           | 103     | 103     | 0        | 0        |
      | drug_strength        | 5       | 5       | 0        | 0        |

  Scenario: Download delta bundle from cache
    When user inspects list of vocabulary release version
    Then it is a list containing:
      | id       | cachedDate              |
      | 20200509 |                         |
      | 20200511 | ~([0-9-]{10}T[0-9:.]+Z) |
      | 20200513 | ~([0-9-]{10}T[0-9:.]+Z) |
      | 20200515 | ~([0-9-]{10}T[0-9:.]+Z) |

    When user generates delta bundle for versions: 20200515 and 20200511
    Then it is a list containing:
      | name                 | ext | rows | path            |
      | CONCEPT_ANCESTOR     | csv | 161  | ~(.+)           |
      | CONCEPT_CLASS        | csv | 4    | ~(.+)           |
      | CONCEPT              | csv | 3    | ~(.+)           |
      | CONCEPT_RELATIONSHIP | csv | 24   | ~(.+)           |
      | CONCEPT_SYNONYM      | csv | 7    | ~(.+)           |
      | DOMAIN               | csv | 3    | ~(.+)           |
      | DRUG_STRENGTH        | csv | 3    | ~(.+)           |
      | RELATIONSHIP         | csv | 6    | ~(.+)           |
      | delta                | sql | 221  | ~(?<sqlPath>.+) |
      | VOCABULARY           | csv | 4    | ~(.+)           |
    When user compare and inspect schemas "vocabulary_20200511" and "vocabulary_20200515"
    Then it is a list containing:
      | name                 | amount1 | amount2 | missing1 | missing2 |
      | concept              | 184     | 184     | 2        | 2        |
      | concept_ancestor     | 319     | 319     | 157      | 157      |
      | concept_class        | 390     | 390     | 3        | 3        |
      | domain               | 47      | 47      | 2        | 2        |
      | concept_relationship | 664     | 668     | 14       | 10       |
      | relationship         | 10      | 10      | 4        | 4        |
      | concept_synonym      | 25      | 24      | 3        | 4        |
      | vocabulary           | 103     | 103     | 3        | 3        |
      | drug_strength        | 5       | 5       | 2        | 2        |
    When run "sqlPath" script on "vocabulary_20200511" schema
    And user compare and inspect schemas "vocabulary_20200511" and "vocabulary_20200515"
    Then it is a list containing:
      | name                 | amount1 | amount2 | missing1 | missing2 |
      | concept              | 184     | 184     | 0        | 0        |
      | concept_ancestor     | 319     | 319     | 0        | 0        |
      | concept_class        | 390     | 390     | 0        | 0        |
      | domain               | 47      | 47      | 0        | 0        |
      | concept_relationship | 668     | 668     | 0        | 0        |
      | relationship         | 10      | 10      | 0        | 0        |
      | concept_synonym      | 24      | 24      | 0        | 0        |
      | vocabulary           | 103     | 103     | 0        | 0        |
      | drug_strength        | 5       | 5       | 0        | 0        |
