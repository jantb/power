## Set project and default region

``` sh
gcloud config set project power-345916
gcloud config set functions/region europe-west1
```

## To deploy

``` sh
gcloud functions deploy power --entry-point Main --runtime java17 --trigger-topic hourly --memory 256MB
```

### Deploy as http function

```
gcloud functions deploy power --entry-point Main --runtime java17 --trigger-http --allow-unauthenticated --memory 256MB --project power-345916
```

## Testing in cloud

```
gcloud pubsub topics publish hourly --message "true"
```

## Check logs

`gcloud functions logs read| tail -r`