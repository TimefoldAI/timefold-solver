curl -X 'POST' \
  'http://localhost:8080/v1/timetables' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "config": {
    "run": {
      "termination": {
        "spentLimit": "PT10S"
      }
    }
  },
  "modelInput": {
    "timeslots": [
      {
        "dayOfWeek": "MONDAY",
        "startTime": "13:45:30.123456789",
        "endTime": "13:45:30.123456789"
      }
    ],
    "rooms": [
      {
        "name": "Room1"
      }
    ],
    "lessons": [
      {
        "id": "Lesson1",
        "subject": "English",
        "teacher": "Mrs. Joos",
        "studentGroup": "1st Grade"
      }
    ]
  }
}'
