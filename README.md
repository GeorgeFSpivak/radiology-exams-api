# Radiology Exams API

A study API built with Java and Spring Boot to register radiology exams, track their status, and keep a history of changes. DICOM files are stored in Orthanc, and exam data is stored in PostgreSQL. Development was guided by GPT 6 Astra (High), GPT 6 Astra (Max), and GPT 6.1 (High), which proposed the architecture, scope, limitations, and test plan, and helped review the code.

## API usage

### 1. Create an exam

`POST /exams`

```json
{
  "code": "EXAM-001"
}
```

The exam starts with the status `REQUESTED`.

### 2. Upload DICOM files

`POST /exams/EXAM-001/dicom`

Send the files as `multipart/form-data` in the `files` field.

Response:

```json
{
  "orthancStudyId": "86d4ba01-57012f1d-f41e693c-a9fd6db2-3d234139",
  "studyInstanceUid": "1.3.6.1.4.1.9590.100.1.1.33330692310366976434447364470364477836",
  "dicomReceivedAt": "2026-10-30T06:36:56.005112Z"
}
```

The IDs and timestamps depend on the study and when the upload takes place. The exam stays in `REQUESTED`.

### 3. Mark the exam as performed

`PATCH /exams/EXAM-001/status`

```json
{
  "newStatus": "EXAM_PERFORMED",
  "changedBy": "technician-01"
}
```

The exam must have a linked DICOM study before this change.

### 4. Start the report

`PATCH /exams/EXAM-001/status`

```json
{
  "newStatus": "REPORT_IN_PROGRESS",
  "changedBy": "radiologist-01"
}
```

### 5. Mark the report as finalized

`PATCH /exams/EXAM-001/status`

```json
{
  "newStatus": "REPORT_FINALIZED",
  "changedBy": "radiologist-01"
}
```

### 6. Get the exam and its history

`GET /exams/EXAM-001`

Returns the exam data, current status, and history of changes.

### Cancel an exam

Only allowed in `REQUESTED`. A linked DICOM study is not required.

`PATCH /exams/EXAM-002/status`

```json
{
  "newStatus": "CANCELED",
  "changedBy": "technician-01"
}
```

## Technologies and tests

Java 21, Spring Boot, Spring Data JPA, PostgreSQL, Flyway, Docker Compose, Orthanc, and Postman.

The project includes unit tests, endpoint tests, and integration tests with PostgreSQL, using JUnit, Mockito, MockMvc, and Testcontainers.

## Limitations

- Uploads are limited to 25 MB per file and 250 MB per request.
- All uploaded files must belong to the same study.
- Each exam can be linked to a study only once, while its status is `REQUESTED`.
- Uploading files does not change the exam status or check whether all study images have been received.
- A failure after uploading files may leave them in Orthanc without a link to an exam in PostgreSQL.
- Cancellation is only allowed in `REQUESTED`. No further status changes are allowed after `CANCELED` or `REPORT_FINALIZED`.

## Running locally

### 1. Clone the repository

```bash
git clone https://github.com/GeorgeFSpivak/radiology-exams-api.git
cd radiology-exams-api
```

### 2. Start PostgreSQL and Orthanc

From the project root:

```bash
docker compose up -d
```

### 3. Start the API

Windows — PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux or macOS:

```bash
./mvnw spring-boot:run
```

### 4. Run the tests

With Docker running, open another terminal in the project root.

Windows:

```powershell
.\mvnw.cmd test
```

Linux or macOS:

```bash
./mvnw test
```

Testcontainers starts a separate PostgreSQL instance for the tests.