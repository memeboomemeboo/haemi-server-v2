package com.memeboo2.haemi.platform.ai.api;

/**
 * 하이라이트 문구에 사용할 수 있는 비진단적 사실.
 *
 * <p>정답률·점수·진단명·다른 어르신과의 비교값은 이 계약에 포함하지 않는다.</p>
 */
public enum WeeklyHighlightFact {
    ORIENTATION_STRENGTH,
    RECALL_STRENGTH,
    LANGUAGE_STRENGTH,
    DELAYED_RECALL_STRENGTH,
    ORIENTATION_SUPPORT,
    RECALL_SUPPORT,
    LANGUAGE_SUPPORT,
    DELAYED_RECALL_SUPPORT;

    public boolean isStrength() {
        return switch (this) {
            case ORIENTATION_STRENGTH, RECALL_STRENGTH, LANGUAGE_STRENGTH, DELAYED_RECALL_STRENGTH -> true;
            case ORIENTATION_SUPPORT, RECALL_SUPPORT, LANGUAGE_SUPPORT, DELAYED_RECALL_SUPPORT -> false;
        };
    }

    public boolean isObservation() {
        return !isStrength();
    }
}
