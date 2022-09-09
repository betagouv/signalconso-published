import boto3
import os
import psycopg2
import random
import string

s3client = boto3.client(
    's3',
    aws_access_key_id=os.getenv("SIGNALCONSO_ACCESS_KEY"),
    aws_secret_access_key=os.getenv("SIGNALCONSO_SECRET_KEY"),
    endpoint_url=os.getenv("SIGNALCONSO_ENDPOINT_URL")
)
BUCKET = os.getenv("SIGNALCONSO_BUCKET")

dbconn = psycopg2.connect(os.getenv("SIGNALCONSO_DB_URI"))
dbconn.set_session(autocommit=True)
dbcur = dbconn.cursor()

genRand = lambda length: ''.join(random.choices(string.ascii_uppercase + string.digits, k=length))

def s3_copy(filename, new_filename):
    print(f"id={filename} => {new_filename}")
    print({'Bucket': BUCKET, 'Key': filename})
    print(s3client.copy_object(
        CopySource={'Bucket': BUCKET, 'Key': filename},
        Bucket=BUCKET,
        Key=new_filename
    ))

dbcur.execute("""SELECT id, filename FROM report_files WHERE storage_filename = id::TEXT""")
rows = list(dbcur.fetchall())
for i, (uid, filename) in enumerate(rows):
    try:
        storage_filename = f"{genRand(12)}_{filename}"
        s3_copy(uid, storage_filename)
        dbcur.execute("""UPDATE report_files SET storage_filename = %s WHERE id = %s""", (storage_filename, uid))
        print(f"Update: {dbcur.rowcount}")
        outcome = "Done"
    except Exception as e:
        outcome = str(e)
    print(f"[{i} / {len(rows) - 1}] {outcome}")
