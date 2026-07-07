package com.example.faultclassifier1

import kotlin.math.*

data class ComplexNumber(
    val real: Double,
    val imag: Double
) {
    fun add(o: ComplexNumber): ComplexNumber {
        return ComplexNumber(real + o.real, imag + o.imag)
    }

    fun subtract(o: ComplexNumber): ComplexNumber {
        return ComplexNumber(real - o.real, imag - o.imag)
    }

    fun multiply(o: ComplexNumber): ComplexNumber {
        return ComplexNumber(
            real * o.real - imag * o.imag,
            real * o.imag + imag * o.real
        )
    }

    fun divide(o: ComplexNumber): ComplexNumber {
        val d = o.real * o.real + o.imag * o.imag
        if (d == 0.0) return ComplexNumber(0.0, 0.0)

        return ComplexNumber(
            (real * o.real + imag * o.imag) / d,
            (imag * o.real - real * o.imag) / d
        )
    }

    fun scale(k: Double): ComplexNumber {
        return ComplexNumber(real * k, imag * k)
    }

    fun absValue(): Double {
        return sqrt(real * real + imag * imag)
    }

    fun angleRad(): Double {
        return atan2(imag, real)
    }

    fun conjugate(): ComplexNumber {
        return ComplexNumber(real, -imag)
    }

    fun expC(): ComplexNumber {
        val e = exp(real)
        return ComplexNumber(e * cos(imag), e * sin(imag))
    }

    fun logC(): ComplexNumber {
        return ComplexNumber(
            ln(absValue()),
            angleRad()
        )
    }

    fun sqrtC(): ComplexNumber {
        val r = absValue()
        val re = sqrt((r + real) / 2.0)
        val imSign = if (imag >= 0.0) 1.0 else -1.0
        val im = imSign * sqrt(max(0.0, (r - real) / 2.0))
        return ComplexNumber(re, im)
    }

    fun sinhC(): ComplexNumber {
        val ez = this.expC()
        val emz = this.scale(-1.0).expC()
        return ez.subtract(emz).scale(0.5)
    }

    fun coshC(): ComplexNumber {
        val ez = this.expC()
        val emz = this.scale(-1.0).expC()
        return ez.add(emz).scale(0.5)
    }

    fun tanhC(): ComplexNumber {
        return sinhC().divide(coshC())
    }

    fun atanhC(): ComplexNumber {
        val one = ComplexNumber(1.0, 0.0)
        val num = one.add(this)
        val den = one.subtract(this)

        return num.divide(den).logC().scale(0.5)
    }

    fun acoshC(): ComplexNumber {
        val one = ComplexNumber(1.0, 0.0)
        val z2minus1 = this.multiply(this).subtract(one)
        return this.add(z2minus1.sqrtC()).logC()
    }
}

object PhasorUtil {

    fun fromMagnitudeAngle(
        magnitude: Double,
        angleDegree: Double
    ): ComplexNumber {

        val angleRad = angleDegree * Math.PI / 180.0

        return ComplexNumber(
            magnitude * cos(angleRad),
            magnitude * sin(angleRad)
        )
    }

    fun expJ(angleRad: Double): ComplexNumber {
        return ComplexNumber(cos(angleRad), sin(angleRad))
    }
}