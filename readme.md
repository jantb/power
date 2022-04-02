gcloud config set project power-345916
gcloud config set functions/region europe-west1
gcloud functions deploy power --entry-point Main --runtime java17 --trigger-topic hourly --memory 256MB
gcloud functions deploy power --entry-point Main --runtime java17 --trigger-http --allow-unauthenticated --memory 256MB --project power-345916