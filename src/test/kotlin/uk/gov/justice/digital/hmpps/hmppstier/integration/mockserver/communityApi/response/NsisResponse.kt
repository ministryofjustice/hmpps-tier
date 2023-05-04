package uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response

import uk.gov.justice.digital.hmpps.hmppstier.integration.mockserver.communityApi.response.domain.NSI

fun nsisResponse(vararg nsis: NSI) = """
  {
    "nsis": [
      ${nsis.joinToString(",") { getNSI(it) }}
    ]
  }
""".trimIndent()

fun getNSI(nsi: NSI) = """
  {
      "nsiId": 1947213,
      "nsiType": {
        "code": "BRE",
        "description": "Breach Request"
      },
      "nsiSubType": {
        "code": "BRE01",
        "description": "Community Order / SSO"
      },
      "nsiOutcome": {
        "code": "${nsi.outcome}",
        "description": "Continued/Fine"
      },
      "nsiStatus": {
        "code": "BRE08",
        "description": "Breach Proven - Order to Continue"
      },
      "statusDateTime": "2020-01-03T15:47:00",
      "referralDate": "2019-11-01",
      "lengthUnit": "Months",
      "nsiManagers": [
        {
          "probationArea": {
            "probationAreaId": 1500001005,
            "code": "N06",
            "description": "NPS South East and Eastern",
            "organisation": {
              "code": "NPS",
              "description": "National Probation Service"
            }
          },
          "team": {
            "code": "N06E51",
            "description": "ENF-BeNCH Enforcement Team",
            "localDeliveryUnit": {
              "code": "N06ENFB",
              "description": "BeNCH Enforcement"
            },
            "teamType": {
              "code": "N06CRT",
              "description": "Court"
            },
            "district": {
              "code": "N06ENFB",
              "description": "BeNCH Enforcement"
            },
            "borough": {
              "code": "N06ENF",
              "description": "Enforcement SEE"
            }
          },
          "staff": {
            "username": "GemmaRobinsonNPS",
            "staffCode": "N06A158",
            "staffIdentifier": 1501265313,
            "staff": {
              "forenames": "Gemma",
              "surname": "Robb"
            }
          },
          "startDate": "2019-11-13",
          "endDate": "2020-01-06"
        },
        {
          "probationArea": {
            "probationAreaId": 1500001019,
            "code": "C13",
            "description": "CPA BeNCH",
            "organisation": {
              "code": "PVD01",
              "description": "Sodexo Justice and NACRO"
            }
          },
          "team": {
            "code": "C13AD3",
            "description": "CRC - HUB Admin HERTS",
            "localDeliveryUnit": {
              "code": "C13HUB",
              "description": "The Hub"
            },
            "teamType": {
              "code": "C13CRC",
              "description": "CRC Team"
            },
            "district": {
              "code": "C13HUB",
              "description": "The Hub"
            },
            "borough": {
              "code": "C13HUB",
              "description": "The Hub"
            }
          },
          "staff": {
            "username": "username",
            "staffCode": "staffCode",
            "staffIdentifier": 123,
            "staff": {
              "forenames": "forename",
              "surname": "surname"
            },
            "teams": [
              {
                "code": "C13H04",
                "description": "CRC - HUB Programmes",
                "localDeliveryUnit": {
                  "code": "C13HUB",
                  "description": "The Hub"
                },
                "teamType": {
                  "code": "C13CRC",
                  "description": "CRC Team"
                },
                "district": {
                  "code": "C13HUB",
                  "description": "The Hub"
                },
                "borough": {
                  "code": "C13HUB",
                  "description": "The Hub"
                }
              }
            ]
          },
          "startDate": "2019-11-01",
          "endDate": "2019-11-13"
        },
        {
          "probationArea": {
            "probationAreaId": 1500001019,
            "code": "C13",
            "description": "CPA BeNCH",
            "organisation": {
              "code": "PVD01",
              "description": "Sodexo Justice and NACRO"
            }
          },
          "team": {
            "code": "C13458",
            "description": "CRC - NHC OM",
            "telephone": "01438 747074",
            "localDeliveryUnit": {
              "code": "HFS401",
              "description": "Hertfordshire"
            },
            "teamType": {
              "code": "C13CRC",
              "description": "CRC Team"
            },
            "district": {
              "code": "HFS401",
              "description": "Hertfordshire"
            },
            "borough": {
              "code": "HFS4CR",
              "description": "Hertfordshire"
            }
          },
          "staff": {
            "username": "username",
            "staffCode": "staffCode",
            "staffIdentifier": 123,
            "staff": {
              "forenames": "forename",
              "surname": "surname"
          },
            "teams": [
              {
                "code": "C13458",
                "description": "CRC - NHC OM",
                "telephone": "01438 747074",
                "localDeliveryUnit": {
                  "code": "HFS401",
                  "description": "Hertfordshire"
                },
                "teamType": {
                  "code": "C13CRC",
                  "description": "CRC Team"
                },
                "district": {
                  "code": "HFS401",
                  "description": "Hertfordshire"
                },
                "borough": {
                  "code": "HFS4CR",
                  "description": "Hertfordshire"
                }
              }
            ]
          },
          "startDate": "2020-01-06"
        }
      ],
      "notes": "02/10/2019 and 01/11/2019\nChecked and accepted\n\nBreach of SSO \nFailed to attend appointment as instructed on 02.10.19 \nFailed to attend appointment as instructed on 01.11.19 \n\nList for St Albans Crown Court \nCJa 2003 - Schedule 12, para 7(1)\n3/1/2020- Order to continue with 3 Months Curfew."
    }
""".trimIndent()
