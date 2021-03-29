package nl.ngti.thrust

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ThrustApplication

fun main(args: Array<String>) {
	runApplication<ThrustApplication>(*args)
}