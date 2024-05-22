package com.bullit.energysimulator.controller

import arrow.core.Either
import arrow.core.leftNel
import arrow.core.right
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.InvalidContractTypeError

enum class ContractType() {
    FIXED, DYNAMIC;

    companion object {
        fun parseContractTypeString(contractTypeString: String): Either<ApplicationErrors, ContractType> =
            try {
                ContractType.valueOf(contractTypeString.uppercase()).right()
            } catch (e: IllegalArgumentException) {
                InvalidContractTypeError("contractTypeString").leftNel()
            }
    }
}