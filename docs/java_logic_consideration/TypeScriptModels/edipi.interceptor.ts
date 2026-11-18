import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { EdipiAuthService } from './edipi-auth.service';

export const edipiInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(EdipiAuthService);
    const edipi = authService.activeEdipi();

    // Attach X-EDIPI header to all requests targeting the MEW backend API
    if (edipi && req.url.includes('/api/v1/mew')) {
        const clonedReq = req.clone({
            setHeaders: {
                'X-EDIPI': edipi,
            },
        });
        return next(clonedReq);
    }

    return next(req);
};
