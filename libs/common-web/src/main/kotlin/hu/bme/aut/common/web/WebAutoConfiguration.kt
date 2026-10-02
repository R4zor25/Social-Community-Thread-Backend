package hu.bme.aut.common.web

import hu.bme.aut.common.web.error.ApiException
import hu.bme.aut.common.web.error.ProblemDetailsHandler
import hu.bme.aut.common.web.security.CurrentUserArgumentResolver
import hu.bme.aut.common.web.upload.UploadArgumentResolver
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(ProblemDetailsHandler::class)
class WebAutoConfiguration : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(CurrentUserArgumentResolver())
        resolvers.add(UploadArgumentResolver())
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Pageable::class)
    @EnableSpringDataWebSupport
    class PagingConfiguration : WebMvcConfigurer {

        /** Every list has one fixed order in its query; a client-chosen sort would reach the query and fail there. */
        override fun addInterceptors(registry: InterceptorRegistry) {
            registry.addInterceptor(object : HandlerInterceptor {
                override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
                    val paged = handler is HandlerMethod && handler.methodParameters.any { it.parameterType == Pageable::class.java }
                    if (paged && request.getParameter("sort") != null) {
                        throw ApiException(HttpStatus.BAD_REQUEST, "Sorting is fixed for every list; use page and size only")
                    }
                    return true
                }
            })
        }

        @Bean
        fun pageableDefaults() = PageableHandlerMethodArgumentResolverCustomizer {
            it.setFallbackPageable(PageRequest.of(0, 20))
            it.setMaxPageSize(100)
        }
    }
}
