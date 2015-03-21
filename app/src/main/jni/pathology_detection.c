#include <jni.h>        /* Standard Library of Java native interface*/
#include <stdlib.h>     /* Standard Library */
#include <complex.h>    /* Standard Library of Complex Numbers */
#include <math.h>       /* Standard Library of Math functions */
#include "com_hsbsoftwares_android_app_healthdiagnostic_nativepack_NativeMethods.h"

JNIEXPORT jint JNICALL Java_com_hsbsoftwares_android_app_healthdiagnostic_nativepack_NativeMethods_pathologyDetection
  (JNIEnv * env, jobject obj, jintArray lumVector) {
        //used in for loops
        int   i = 0;
        float f = 0;

        //other variables used in the method
        double  complex exponent, tot;//complex number definition in C    ....leaving it uninitialized??
        double  imm;//how can i initialize it?????   or leaving it uninitialized??
        double  max = 0;

        float ampiezza; //how can i initialize it????? ampiezza = 0 is not the good choice i think...or leaving it uninitialized??
        float fc = 25.0;
        float fStim = 0.1, fMax = 0.1, iMax = 0.1;

        //Using jni methods
        //to get array length
        jsize lumSize = (*env)->GetArrayLength(env, lumVector);
        // obtain a pointer to the elements of the array
        jint *lum = (*env)->GetIntArrayElements(env, lumVector, 0);

        for (i = 0; i < lumSize; i++) {
            if (lum[i] > 40000) lum[i] = max; //deleting big movements
            if (lum[i] > max) max = lum[i];
        }

        for (i = 0; i < lumSize; i++) {
            lum[i] = lum[i]/max;
            if (lum[i] < 0.15) lum[i]=0; //deleting residual noise 0.15
        }

        for (f = 0.25; f <= 2; f += 0.05) {
            tot = 0;
            imm = -2*M_PI*f*(1/fc);

            for (i = 0; i < lumSize; i++) {
                exponent = 0 + i*I;
                tot = tot + lum[i]*exp(exponent);
            }

            //fStim = pow(cabs(tot),2);////////////non funziona non so perchè :(
  			fStim = pow(sqrt(creal(tot)*creal(tot) + cimag(tot)*cimag(tot)),2);

            if ((fStim > fMax)) {
                fMax = fStim;
                iMax = f;
            }
        }

        tot = 0;

        imm = -2*M_PI*iMax*(1/fc);

        for (i = 0; i < lumSize; i++) {
            exponent = 0 + i*I;
            tot = tot + lum[i]*exp(exponent);
        }

        //cabs = abs for an complex number in C.
        //ampiezza = 2*cabs(tot)/(lumSize);not workingggg :(
        ampiezza = 2*(sqrt(creal(tot)*creal(tot) + cimag(tot)*cimag(tot)))/(lumSize);

        //Releasing array because C can make a copy which java garbage collector can't see.
        //jni method.
        (*env)->ReleaseIntArrayElements(env, lumVector, lum, 0);

        if(ampiezza < 0.073) return 1;
        else return 0;
  }