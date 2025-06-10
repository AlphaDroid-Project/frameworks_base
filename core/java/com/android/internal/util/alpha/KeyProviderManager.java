/*
 * SPDX-FileCopyrightText: 2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.internal.util.alpha;

import android.app.ActivityThread;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.text.TextUtils;

import com.android.internal.R;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manager class for handling keybox providers.
 * @hide
 */
public final class KeyProviderManager {
    private static final String TAG = "KeyProviderManager";
    private static final IKeyboxProvider PROVIDER = new DefaultKeyboxProvider();

    private static final String DEFAULT_KEYBOX="{\"EC.PRIV\":\"MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgntUEup/NdXx9iKtuwHKx177YQxIvhaPkEQpAOPjHxxCgCgYIKoZIzj0DAQehRANCAARZAoMkSNgXn6MRY0jM3t/7FEOBOVWXXEgVXO21wK394TOG3aVq4Ti6LGVTJG3O2nEZbXSCWOIcA+dyxQ3hFZHB\",\"EC.CERT_1\":\"MIICJDCCAaugAwIBAgIKAZZYMGAohlAYITAKBggqhkjOPQQDAjApMRkwFwYDVQQFExA1NDRjMTRlMTJkYzgyMGYzMQwwCgYDVQQMDANURUUwHhcNMTgwNDE4MjIzOTM0WhcNMjgwNDE1MjIzOTM0WjApMRkwFwYDVQQFExA0Nzc3YzQwZDJhMWQyNjVmMQwwCgYDVQQMDANURUUwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARZAoMkSNgXn6MRY0jM3t/7FEOBOVWXXEgVXO21wK394TOG3aVq4Ti6LGVTJG3O2nEZbXSCWOIcA+dyxQ3hFZHBo4G6MIG3MB0GA1UdDgQWBBSKhqG8P0H4JGpd7rJUc9aXWMn4RDAfBgNVHSMEGDAWgBTSfnB7oefBbLt1RqEuwdiFUGy1bzAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwICBDBUBgNVHR8ETTBLMEmgR6BFhkNodHRwczovL2FuZHJvaWQuZ29vZ2xlYXBpcy5jb20vYXR0ZXN0YXRpb24vY3JsLzAxOTY1ODMwNjAyODg2NTAxODIxMAoGCCqGSM49BAMCA2cAMGQCMGpnxr5EMyG9NdyEDnfbug1sLoWwceLZjRHPFTHfAioaW9/VTdZiv7Y0LeTi71EJlgIwF+/lhjz64sYvRQ7dcHHtkwuJ1sq15NGIsArG9azYUxqKNQ6ZxukLWWLbPiQaKY6A\",\"EC.CERT_2\":\"MIID0TCCAbmgAwIBAgIKA4gmZ2BliZaFkzANBgkqhkiG9w0BAQsFADAbMRkwFwYDVQQFExBmOTIwMDllODUzYjZiMDQ1MB4XDTE4MDQxODIyMjE0MVoXDTI4MDQxNTIyMjE0MVowKTEZMBcGA1UEBRMQNTQ0YzE0ZTEyZGM4MjBmMzEMMAoGA1UEDAwDVEVFMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE/WvRRlHZDCrhcd0319b9SehNankl3p/k79NX1ZmZHD50yZ7lCb0lENs0FGn4SL+l+WrKSKTbCfUO3wGFhFg1QTiM62IRGBhM1/tqC8fP50R9gqQ3d6ajJEX9bPLp7AE8o4G2MIGzMB0GA1UdDgQWBBTSfnB7oefBbLt1RqEuwdiFUGy1bzAfBgNVHSMEGDAWgBQ2YeEAfIgFCVGLRGxH/xpMyepPEjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwICBDBQBgNVHR8ESTBHMEWgQ6BBhj9odHRwczovL2FuZHJvaWQuZ29vZ2xlYXBpcy5jb20vYXR0ZXN0YXRpb24vY3JsL0U4RkExOTYzMTREMkZBMTgwDQYJKoZIhvcNAQELBQADggIBAJnYLaTbhN2Ly91kxLj/vljTcqlCV9cgVYteJLJPWLNdWFZu167qppnmFDKn/VDV24v3XbJb7jqzMBrE1V+DN72lFfREHYDyJM8FITF0CbIuOTwTSLKRV26Ogm8esFrCjPoWC6OLTsHLSGfv5KQO1M0KpjqkOywBxgCCHCbI+BotefY2Kr/JTJIIEOq5ra0QcuAZTQULMNSdYzMQZawnGFH4XeINKiScoISch939//APkBbNXHmdpM7u5t2B3dnCQekXtX3qMbSAGhV6OSoM4bRY23tiLToYzXQyysE8sqDVfmaLWXduh+xsodvc0EDsE8fS4MIS5VXWVBelS+9j0AjAMCNp24G8bMG2EdM7xxK+UciAUwgLwFFxVDFpCzdPyhKIdpTPK6UVOInS9qklTXpwIvGDIwsowAS005s+D5rGo1qZD0mwGXZRKtqF64nTqpmOXWg7baRufUjMerroggY4XgZ14vtLOcsiQevjL7nNH4G8SekWL4uIRH7AxBKaIqPR69XlEFwZdgn7e0ED7t8MLJ/f1/todJEaQOHk2uysbpr0SIZdtWqS4w7IQRpGo4RaiKl7rYHVvXiuBPqt2S8MeNyPi225Yq/gWZSLtPcg2Dl45aFzqw72egCt5b20ijNGnUIUQP7snMHKL+4JoGl86LC7xtbpdLKvcJO9Vv3u\",\"EC.CERT_3\":\"MIIFYDCCA0igAwIBAgIJAOj6GWMU0voYMA0GCSqGSIb3DQEBCwUAMBsxGTAXBgNVBAUTEGY5MjAwOWU4NTNiNmIwNDUwHhcNMTYwNTI2MTYyODUyWhcNMjYwNTI0MTYyODUyWjAbMRkwFwYDVQQFExBmOTIwMDllODUzYjZiMDQ1MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAr7bHgiuxpwHsK7Qui8xUFmOr75gvMsd/dTEDDJdSSxtf6An7xyqpRR90PL2abxM1dEqlXnf2tqw1Ne4Xwl5jlRfdnJLmN0pTy/4lj4/7tv0Sk3iiKkypnEUtR6WfMgH0QZfKHM1+di+y9TFRtv6y//0rb+T+W8a9nsNL/ggjnar86461qO0rOs2cXjp3kOG1FEJ5MVmFmBGtnrKpa73XpXyTqRxB/M0n1n/W9nGqC4FSYa04T6N5RIZGBN2z2MT5IKGbFlbC8UrW0DxW7AYImQQcHtGl/m00QLVWutHQoVJYnFPlXTcHYvASLu+RhhsbDmxMgJJ0mcDpvsC4PjvB+TxywElgS70vE0XmLD+OJtvsBslHZvPBKCOdT0MS+tgSOIfga+z1Z1g7+DVagf7quvmag8jfPioyKvxnK/EgsTUVi2ghzq8wm27ud/mIM7AY2qEORR8Go3TVB4HzWQgpZrt3i5MIlCaY504LzSRiigHCzAPlHws+W0rB5N+er5/2pJKnfBSDiCiFAVtCLOZ7gLiMm0jhO2B6tUXHI/+MRPjy02i59lINMRRev56GKtcd9qO/0kUJWdZTdA2XoS82ixPvZtXQpUpuL12ab+9EaDK8Z4RHJYYfCT3Q5vNAXaiWQ+8PTWm2QgBR/bkwSWc+NpUFgNPN9PvQi8WEg5UmAGMCAwEAAaOBpjCBozAdBgNVHQ4EFgQUNmHhAHyIBQlRi0RsR/8aTMnqTxIwHwYDVR0jBBgwFoAUNmHhAHyIBQlRi0RsR/8aTMnqTxIwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAYYwQAYDVR0fBDkwNzA1oDOgMYYvaHR0cHM6Ly9hbmRyb2lkLmdvb2dsZWFwaXMuY29tL2F0dGVzdGF0aW9uL2NybC8wDQYJKoZIhvcNAQELBQADggIBACDIw41L3KlXG0aMiS//cqrG+EShHUGo8HNsw30W1kJtjn6UBwRM6jnmiwfBPb8VA91chb2vssAtX2zbTvqBJ9+LBPGCdw/E53Rbf86qhxKaiAHOjpvAy5Y3m00mqC0w/Zwvju1twb4vhLaJ5NkUJYsUS7rmJKHHBnETLi8GFqiEsqTWpG/6ibYCv7rYDBJDcR9W62BW9jfIoBQcxUCUJouMPH25lLNcDc1ssqvC2v7iUgI9LeoM1sNovqPmQUiG9rHli1vXxzCyaMTjwftkJLkf6724DFhuKug2jITV0QkXvaJWF4nUaHOTNA4uJU9WDvZLI1j83A+/xnAJUucIv/zGJ1AMH2boHqF8CY16LpsYgBt6tKxxWH00XcyDCdW2KlBCeqbQPcsFmWyWugxdcekhYsAWyoSf818NUsZdBWBaR/OukXrNLfkQ79IyZohZbvabO/X+MVT3rriAoKc8oE2Uws6DF+60PV7/WIPjNvXySdqspImSN78mflxDqwLqRBYkA3I75qppLGG9rp7UCdRjxMl8ZDBld+7yvHVgt1cVzJx9xnyGCC23UaicMDSXYrB4I4WHXPGjxhZuCuPBLTdOLU8YRvMYdEvYebWHMpvwGCF6bAx3JBpIeOQ1wDB5y0USicV3YgYGmi+NZfhA4URSh77Yd6uuJOJENRaNVTzk\",\"RSA.PRIV\":\"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\",\"RSA.CERT_1\":\"MIIFETCCAvmgAwIBAgIKFIGQWXmBkAIxhDANBgkqhkiG9w0BAQsFADApMRkwFwYDVQQFExA1NDRjMTRlMTJkYzgyMGYzMQwwCgYDVQQMDANURUUwHhcNMTgwNDE4MjIzOTIyWhcNMjgwNDE1MjIzOTIyWjApMRkwFwYDVQQFExA0Nzc3YzQwZDJhMWQyNjVmMQwwCgYDVQQMDANURUUwggGiMA0GCSqGSIb3DQEBAQUAA4IBjwAwggGKAoIBgQDdiLarh0PFyifW4VBiq/1yHnIh9yWDyeYLXdDgcb5NgL+Foy9oIya4kqR1zVkJ5jo8onUKgmwiKtEcH39XK6oJDtzTAWBf1e8FRLI2hg4rtdJ/9YphFuIaVX2jKFJs3u/5KdR7Ntin2dddVpAvl1RGzzMqLqE0/gCQpKY2m9UnMo2h9EUMTh+Lk69B0H3xN19dNXP03LF9xJpfvY7imwn6HQPFRMSPx+loIBEUcdxwNTDhfpwfdYRdMJRHRqxghIw1WzR4i+WLKCLQyqyj3ZEpcmaSynKGLThZUBXC17nsRKEe/ZggJbAqxfR39EY0d7Pf6B+RHxQCouzsbYv7iSrzPa7Hn3K23CEE6o8IaiaWpW/8iIYHIB/5nQ0xCYICWRJMvEYwdu6Fh1AaJkWSBOq/8BbQRQH9kegbMWE+Sc6KzjWiqdK+pl0ILDYSqfNijHJQWRh2y4VsV/K9vIpWQ507D0xtCD+e69e3yY9ejj+tg7K5Lpts7DGwc0RKnEca6WECAwEAAaOBujCBtzAdBgNVHQ4EFgQUPC4+Ga0tzpl6C2FmbZWCNGY33XYwHwYDVR0jBBgwFoAUZgI31/afNWqDEgrvgZUAMAqPgAEwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAgQwVAYDVR0fBE0wSzBJoEegRYZDaHR0cHM6Ly9hbmRyb2lkLmdvb2dsZWFwaXMuY29tL2F0dGVzdGF0aW9uL2NybC8xNDgxOTA1OTc5ODE5MDAyMzE4NDANBgkqhkiG9w0BAQsFAAOCAgEAHDU/V2FrFB1HlpRmn0hTsANOTTWbiBPlsqaYbksnJQYii9YbeOZXKVaomrJJFlhH798oTWNDV5T/jKGGGPP7TwPEX13zoy3zwaCsOJnnSMA5RGuQDK+efL0gy7nKd7lo3Jdn+/a45qupXDPzL3J+e9GQLOpKWLfwCmlSZA6G7aYQm/Ly9xxIgqlrKSACQwQNbkBvG64au7PCMk/9VLMI8bPfHWofBX6b4+UR2up8IeAMrCXAvJ5FN+zo3wuYSvjcyV8sCOy/XDS9iHRq00eQDmcrwvyizBuq/GsbelljwkFxMsRa9PraandCx4eSBSjLXzU0ok5NsYgSHWk7zJijcshn1VI3o5zDm85eBX2yOXy6KrKiKfY0HAFxc1EIPi6UbxoOvp9kRX9wyTQ4TS62kJDhmtBh+da8NJavKc1TEGGoNn/fwgo4iLQGVTEKOHJFD5z29n48+1OvSLibtsZdnVZaKF3g1wNgRNzAZYERdkXYqtgwFp6k0ui/IiulMuod9deNyHF2iiTHMa0oEmVQThfFj/pV8C8Q16psBwWNfoUG5mzPcPhdQwHqzHfvHB9APqr4nVApjtQDhU96tKmOVV0fJAhPnUb4J2VmFjfWqcNRzIEdrslW++ESJwRhNZiuMFm4wZWCPP1vgwaUwXGDnQQWZ54TVM9PT3GnqEFGHgs=\",\"RSA.CERT_2\":\"MIIFfzCCA2egAwIBAgIKA4gmZ2BliZaFkjANBgkqhkiG9w0BAQsFADAbMRkwFwYDVQQFExBmOTIwMDllODUzYjZiMDQ1MB4XDTE4MDQxODIyMjEyNFoXDTI4MDQxNTIyMjEyNFowKTEZMBcGA1UEBRMQNTQ0YzE0ZTEyZGM4MjBmMzEMMAoGA1UEDAwDVEVFMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA5SybDthoSvR9RVh6MJeeIe0PXYL/zcKknuUpKmSxf1o8IXQCqxsFfGt1snB44XhVMocp4CVzESN92l6L1jt0UWOQuu21DhraTWDF8t4FKqMpYTf1tumG2MZAh9ZU8JCO5sDUKejqF/GGVWtd3Mf/eUoaTkRXgiWHIVurmcACfj7i2ssj3yGHmRRysf6O/QxBhdv8FjNUvFr9TeuDkw03MmpTYwaDpMc9DYgRdx7xNI7g++uGOwR1QIXaF6LNrjZKgyd1zOU99VpUXJZrSAc8UebhTIpuEVInNE+QdN7px8Rm0ACB686eYdBWN162kRgcYkGoB+0Hz8NzrbgYoK5Yvf+OT3DiylKEISFnmjBwQMTVgipat3z02YhTEc2m25WW1PcsCAk3oEKT3BePFcUfuOlbkH84qxsVNjSreZQ1lUZTppiBkndXTzEQK1zkf/s2iS7hwtVPyN/x3xBgZq4Er5qZ0LVOyHSR92fG2nzwqvZ5Nr+9AgKxB4RyVdXB6hLVs2SqXvV3vCl9YkrKle+jj3ZKpmPdlaRNOct4BA9tFEjmaUdTmokZiCix5jgFSEgEzoBozwMdQSyrQV+n/t3zjitb2uTvR5ouO9+1E+X/N4UMp6Z1Z6zPpNnUD6rQ4CQ1TQ6JAyRgEma+mkcJKpShDDqwui8DYW3qPrKL3xzrvwkCAwEAAaOBtjCBszAdBgNVHQ4EFgQUZgI31/afNWqDEgrvgZUAMAqPgAEwHwYDVR0jBBgwFoAUNmHhAHyIBQlRi0RsR/8aTMnqTxIwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAgQwUAYDVR0fBEkwRzBFoEOgQYY/aHR0cHM6Ly9hbmRyb2lkLmdvb2dsZWFwaXMuY29tL2F0dGVzdGF0aW9uL2NybC9FOEZBMTk2MzE0RDJGQTE4MA0GCSqGSIb3DQEBCwUAA4ICAQCR5cJJbSBxcTsLpQb+f8LE6tVfhl9/rPtTl0FgIhrMU4OxeCLIt8faQKIsu4M12ae53oyIm5PqfM5hZlkj/elaqz/1ErWlOvaE0Hb6GaCUGTk0xClJHglW9wYCcKJf7w62eoTKvDlO92gCxpXbhnXoxeWr5NzJ0LRdhcFNr9AiCnE2hypFqm+oQAkK6EDmbpxjiB519+5SFUx92Pb48YT+Jk4GrMrB41SFZYNJ2vWmApqVfF9emU0jtpw6UDswRt74EbCqwWWzv1QZWDrlAW4FrK9DbwiVNjvLHCJcoN/A+s2fSP9MHnrGq2FIzzRh0ZjaQjxSOreGyCZRTuDlcxrqywhJq827o+zdR8SD2Y1jr3zuWjjCvBRk5rXZqhAPmd8n4bmwH77tlVPkaIS1mpEhsEjPf2ibGfEZ2Q+10a3DqAzwFz18XLfYun/kLi5JnAvVNQy7K97an/D/NNawPQX/qz7rFmi/nJ1DcgKTLTwbSmoYeQ/L5hZvvsnybKhhRW6emSljhwAdrBHsoX0hlC2vgnIoCNmf2Ew2JOvml9Z7OmbjtJIBWZxiJPcvMr6iM1G++fbbgqpVvnH1O1Tm+gzBvh+YirFdtbFp73lcCiI7pA0ZWyTl2L1mWQ3pEHNII+GCYLVjjWVy9pvR0G0y/SFwQSTFY3Auedlpj0Zzmg0O4A==\",\"RSA.CERT_3\":\"MIIFYDCCA0igAwIBAgIJAOj6GWMU0voYMA0GCSqGSIb3DQEBCwUAMBsxGTAXBgNVBAUTEGY5MjAwOWU4NTNiNmIwNDUwHhcNMTYwNTI2MTYyODUyWhcNMjYwNTI0MTYyODUyWjAbMRkwFwYDVQQFExBmOTIwMDllODUzYjZiMDQ1MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAr7bHgiuxpwHsK7Qui8xUFmOr75gvMsd/dTEDDJdSSxtf6An7xyqpRR90PL2abxM1dEqlXnf2tqw1Ne4Xwl5jlRfdnJLmN0pTy/4lj4/7tv0Sk3iiKkypnEUtR6WfMgH0QZfKHM1+di+y9TFRtv6y//0rb+T+W8a9nsNL/ggjnar86461qO0rOs2cXjp3kOG1FEJ5MVmFmBGtnrKpa73XpXyTqRxB/M0n1n/W9nGqC4FSYa04T6N5RIZGBN2z2MT5IKGbFlbC8UrW0DxW7AYImQQcHtGl/m00QLVWutHQoVJYnFPlXTcHYvASLu+RhhsbDmxMgJJ0mcDpvsC4PjvB+TxywElgS70vE0XmLD+OJtvsBslHZvPBKCOdT0MS+tgSOIfga+z1Z1g7+DVagf7quvmag8jfPioyKvxnK/EgsTUVi2ghzq8wm27ud/mIM7AY2qEORR8Go3TVB4HzWQgpZrt3i5MIlCaY504LzSRiigHCzAPlHws+W0rB5N+er5/2pJKnfBSDiCiFAVtCLOZ7gLiMm0jhO2B6tUXHI/+MRPjy02i59lINMRRev56GKtcd9qO/0kUJWdZTdA2XoS82ixPvZtXQpUpuL12ab+9EaDK8Z4RHJYYfCT3Q5vNAXaiWQ+8PTWm2QgBR/bkwSWc+NpUFgNPN9PvQi8WEg5UmAGMCAwEAAaOBpjCBozAdBgNVHQ4EFgQUNmHhAHyIBQlRi0RsR/8aTMnqTxIwHwYDVR0jBBgwFoAUNmHhAHyIBQlRi0RsR/8aTMnqTxIwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAYYwQAYDVR0fBDkwNzA1oDOgMYYvaHR0cHM6Ly9hbmRyb2lkLmdvb2dsZWFwaXMuY29tL2F0dGVzdGF0aW9uL2NybC8wDQYJKoZIhvcNAQELBQADggIBACDIw41L3KlXG0aMiS//cqrG+EShHUGo8HNsw30W1kJtjn6UBwRM6jnmiwfBPb8VA91chb2vssAtX2zbTvqBJ9+LBPGCdw/E53Rbf86qhxKaiAHOjpvAy5Y3m00mqC0w/Zwvju1twb4vhLaJ5NkUJYsUS7rmJKHHBnETLi8GFqiEsqTWpG/6ibYCv7rYDBJDcR9W62BW9jfIoBQcxUCUJouMPH25lLNcDc1ssqvC2v7iUgI9LeoM1sNovqPmQUiG9rHli1vXxzCyaMTjwftkJLkf6724DFhuKug2jITV0QkXvaJWF4nUaHOTNA4uJU9WDvZLI1j83A+/xnAJUucIv/zGJ1AMH2boHqF8CY16LpsYgBt6tKxxWH00XcyDCdW2KlBCeqbQPcsFmWyWugxdcekhYsAWyoSf818NUsZdBWBaR/OukXrNLfkQ79IyZohZbvabO/X+MVT3rriAoKc8oE2Uws6DF+60PV7/WIPjNvXySdqspImSN78mflxDqwLqRBYkA3I75qppLGG9rp7UCdRjxMl8ZDBld+7yvHVgt1cVzJx9xnyGCC23UaicMDSXYrB4I4WHXPGjxhZuCuPBLTdOLU8YRvMYdEvYebWHMpvwGCF6bAx3JBpIeOQ1wDB5y0USicV3YgYGmi+NZfhA4URSh77Yd6uuJOJENRaNVTzk\"}";
    public static IKeyboxProvider getProvider() {
        return PROVIDER;
    }

    public static boolean isKeyboxAvailable() {
        return PROVIDER.hasKeybox();
    }

    private static void dlog(String msg) {
        if (SystemProperties.getBoolean("persist.sys.keybox_debug", false)) {
            Log.d(TAG, msg);
        }
    }

    private static class DefaultKeyboxProvider implements IKeyboxProvider {
        private final Map<String, String> keyboxData = new HashMap<>();

        private DefaultKeyboxProvider() {
            try {
                Context context = ActivityThread.currentApplication().getApplicationContext();
                
                if (context == null) return;

                String json = Settings.System.getString(context.getContentResolver(), "custom_keybox_data");

                if (TextUtils.isEmpty(json)) {
                    json = DEFAULT_KEYBOX;
                }

                JSONObject keyboxJson = new JSONObject(json);
                Iterator<String> keys = keyboxJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    keyboxData.put(key, keyboxJson.getString(key));
                }

                if (!hasKeybox()) {
                    dlog("Incomplete keybox data loaded");
                    logMissingKeys();
                } else {
                    logLoadedKeys();
                }

            } catch (Exception e) {
                dlog("Error retrieving keybox from settings: " + e.getMessage());
            }
        }

        private void logLoadedKeys() {
            dlog("Successfully loaded keybox data:");
            for (String key : Arrays.asList(
                    "EC.PRIV", "EC.CERT_1", "EC.CERT_2", "EC.CERT_3",
                    "RSA.PRIV", "RSA.CERT_1", "RSA.CERT_2", "RSA.CERT_3")) {
                String value = keyboxData.get(key);
                if (value != null) {
                    dlog(key + ": " + value);
                }
            }
        }

        private void logMissingKeys() {
            for (String key : Arrays.asList(
                    "EC.PRIV", "EC.CERT_1", "EC.CERT_2", "EC.CERT_3",
                    "RSA.PRIV", "RSA.CERT_1", "RSA.CERT_2", "RSA.CERT_3")) {
                if (!keyboxData.containsKey(key)) {
                    dlog("Missing key: " + key);
                }
            }
        }

        @Override
        public boolean hasKeybox() {
            return Arrays.asList("EC.PRIV", "EC.CERT_1", "EC.CERT_2", "EC.CERT_3",
                    "RSA.PRIV", "RSA.CERT_1", "RSA.CERT_2", "RSA.CERT_3")
                    .stream()
                    .allMatch(keyboxData::containsKey);
        }

        @Override
        public String getEcPrivateKey() {
            return keyboxData.get("EC.PRIV");
        }

        @Override
        public String getRsaPrivateKey() {
            return keyboxData.get("RSA.PRIV");
        }

        @Override
        public String[] getEcCertificateChain() {
            return getCertificateChain("EC");
        }

        @Override
        public String[] getRsaCertificateChain() {
            return getCertificateChain("RSA");
        }

        private String[] getCertificateChain(String prefix) {
            return new String[]{
                    keyboxData.get(prefix + ".CERT_1"),
                    keyboxData.get(prefix + ".CERT_2"),
                    keyboxData.get(prefix + ".CERT_3")
            };
        }
    }
}
