package hu.bme.aut.common.web

import hu.bme.aut.common.web.error.ProblemDetailsHandler
import hu.bme.aut.common.web.security.CurrentUserArgumentResolver
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(ProblemDetailsHandler::class)
class WebAutoConfiguration : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(CurrentUserArgumentResolver())
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Pageable::class)
    @EnableSpringDataWebSupport
    class PagingConfiguration {
        @Bean
        fun pageableDefaults() = PageableHandlerMethodArgumentResolverCustomizer {
            it.setFallbackPageable(PageRequest.of(0, 20))
            it.setMaxPageSize(100)
        }
    }
}
