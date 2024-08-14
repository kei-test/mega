package GInternational.server.api.repository;


import GInternational.server.api.entity.CasinoTransaction;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;


import static GInternational.server.api.entity.QCasinoTransaction.casinoTransaction;
import static GInternational.server.api.entity.QUser.user;

@RequiredArgsConstructor
public class CasinoRepositoryImpl implements CasinoRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    @Override
    public Page<CasinoTransaction> findByUserIdAndCasinoTransaction(Long userId, String description, Pageable pageable) {
        List<CasinoTransaction> results = queryFactory.selectFrom(casinoTransaction)
                .innerJoin(casinoTransaction.user, user)
                .where(user.id.eq(userId).and(casinoTransaction.description.eq(description)))
                .orderBy(casinoTransaction.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return new PageImpl<>(results,pageable,results.size());
    }

    @Override
    public Long countByUserId(Long userId,String description) {
        return (long) queryFactory.selectFrom(casinoTransaction)
                .innerJoin(casinoTransaction.user,user)
                .where(user.id.eq(userId).and(casinoTransaction.description.eq(description)))
                .fetch().size();
    }

    @Override
    public List<CasinoTransaction> findByCasinoTransaction(String description, LocalDate startDate, LocalDate endDate) {
        return queryFactory.selectFrom(casinoTransaction)
                .where(descriptionEq(description),
                        casinoTransaction.processedAt.between(startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
                .orderBy(casinoTransaction.processedAt.desc())
                .fetch();
    }

    private BooleanExpression descriptionEq(String description) {
        return description != null ? casinoTransaction.description.eq(description) : null;
    }
}
