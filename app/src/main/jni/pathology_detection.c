#include <jni.h>        /* Standard Library of Java native interface*/
#include <stdlib.h>     /* Standard Library */
#include <complex.h>    /* Standard Library of Complex Numbers */
#include <math.h>       /* Standard Library of Math functions */
#include <android/log.h>
#include "com_hsbsoftwares_android_app_healthdiagnostic_nativepack_NativeMethods.h"

#define  LOG_TAG    "pathology_detection.c"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
// If you want you can add other log definition for info, warning etc

JNIEXPORT jint JNICALL Java_com_hsbsoftwares_android_app_healthdiagnostic_nativepack_NativeMethods_pathologyDetection
  (JNIEnv * env, jobject obj, jintArray lumVector) {
        //used in for loops
        int     i = 0;
        float   f = 0;

        //other variables used in the method
        double  complex /*exponent,*/ tot;//complex number definition in C    ....leaving it uninitialized??
        double  imm;//how can i initialize it?????   or leaving it uninitialized??
        double  max = 0;
        double  exponent;

        float   ampiezza; //how can i initialize it????? ampiezza = 0 is not the good choice i think...or leaving it uninitialized??

        float   fStim = 0.1, fMax = 0.1, iMax = 0.1;

        //Using jni methods
        //to get array length
        jsize lumSize = (*env)->GetArrayLength(env, lumVector);
        // obtain a pointer to the elements of the array
        jint *lum = (*env)->GetIntArrayElements(env, lumVector, 0);

        float   fc = lumSize/10;
        //Debugging
        LOGD("fc = %.4f\n", fc);

        LOGD("Lum vector size = %d\n", lumSize);
        for(i = 0; i < lumSize; i++) {
            LOGD("[%d] = %d\n", i, lum[i]);
        }

        for (i = 0; i < lumSize; i++) {
            if (lum[i] > 80000) lum[i] = max; //deleting big movements max è variabile??????????????????????????????????????????????? nn va 0??
            if (lum[i] > max) max = lum[i];
        }

        /*//Debugging
        LOGD("Normalized Lum vector:\n");////////////////its not normalizedddddddddddddddddd!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        for (i = 0; i < lumSize; i++) {
            lum[i] = lum[i]/max;
            //if (lum[i] < 0.15) lum[i]=0; //deleting residual noise 0.15
            LOGD("[%d] = %d\n", i, lum[i]);
        }*/
        double normalizedLum [lumSize];
        //Debugging
        LOGD("Normalized Lum vector:\n");
        for (i = 0; i < lumSize; i++) {
            normalizedLum[i] = (double)(lum[i]/max);
            if (lum[i] < 0.15) lum[i]=0; //deleting residual noise 0.15
            LOGD("[%d] = %.4lf\n", i, normalizedLum[i]);
        }
        for (f = 0.25; f <= 2; f += 0.05) {
            tot = 0;
            imm = -2*M_PI*f*(1/fc);
            //LOGD("imm 1 = %.2lf\n", imm);

            for (i = 0; i < lumSize; i++) {
                //exponent = imm*i*I;
                exponent = imm*(i+1);//Fi
                //LOGD("Exponent = %.2lf %+.2lfi\n", creal(exponent), cimag(exponent));
                //tot = tot + lum[i]*exp(exponent);
                tot = tot + normalizedLum[i]*(cos(exponent) + sin(exponent) * I);
                //LOGD("Tot in loop 1 = %.2lf %+.2lfi\n", creal(tot), cimag(tot));
            }
            LOGD("Tot in loop 2 = %.2lf %+.2lfi\n", creal(tot), cimag(tot));

            //fStim = pow(cabs(tot),2);////////////non funziona non so perchè :(
  			//fStim = pow(sqrt(creal(tot)*creal(tot) + cimag(tot)*cimag(tot)), 2);
  			double fStim2 = (creal(tot)*creal(tot) + cimag(tot)*cimag(tot)); //è uguale a fstim
  			//LOGD("fstim = %.2f\n", fStim);
  			LOGD("fstim2 = %.2f\n", fStim);

            if ((fStim2 > fMax)) {
                fMax = fStim2;
                iMax = f;
            }
        }

        tot = 0;

        LOGD("iMax = %.2f\n", iMax);

        imm = -2*M_PI*iMax*(1/fc);
        //Debugging
        LOGD("imm 2 = %+.2f\n", imm);

        for (i = 0; i < lumSize; i++) {
            //exponent = imm*i*I;
            exponent = imm*(i+1);
            tot = tot + normalizedLum[i]*(cos(exponent) + sin(exponent) * I);
        }

        //Debugging
        LOGD("Tot (N° complesso) = %.2f %+.2fi\n", creal(tot), cimag(tot));
        //cabs = abs for an complex number in C.
        //ampiezza = 2*cabs(tot)/(lumSize);not workingggg :(
        ampiezza = (2*(sqrt(creal(tot)*creal(tot) + cimag(tot)*cimag(tot))))/(lumSize);
        //Debugging
        LOGD("Ampiezza = %.4f\n", ampiezza);
        LOGD("Ampiezza 2 = %.4f\n", lumSize*pow(ampiezza,2));
        //Releasing array because C can make a copy which java garbage collector can't see.
        //jni method.
        (*env)->ReleaseIntArrayElements(env, lumVector, lum, 0);

        //if(lumSize*pow(ampiezza,2) > 1.5)
        if( ampiezza > 0.1) return 1;
        else return 0;
  }