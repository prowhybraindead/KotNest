from __future__ import annotations

from pydantic import BaseModel, Field


class Subscription(BaseModel):
    id: int = Field(default=0)
    name: str
    amount: float
    currency: str = Field(default="VND")
    categoryId: int
    billingCycle: str
    customCycleValue: int = Field(default=1)
    nextDueDate: int
    paymentMethodId: int | None = None
    isAutoRenew: bool = Field(default=True)
    status: str = Field(default="Upcoming")
    importance: str = Field(default="Medium")
    note: str | None = None
    managementUrl: str | None = None
    isPaused: bool = Field(default=False)
    createdAt: int | None = None
    updatedAt: int | None = None


class ExchangeRateResponse(BaseModel):
    baseCurrency: str
    targetCurrency: str
    rate: float
    sourceDate: str
    provider: str
    cached: bool


class AiInsightResponse(BaseModel):
    brandName: str
    provider: str
    model: str
    filtered: bool
    insight: str
    actions: list[str]
    generatedAt: int


class AiChatMessage(BaseModel):
    role: str
    content: str


class AiChatRequest(BaseModel):
    provider: str | None = None
    model: str | None = None
    message: str
    history: list[AiChatMessage] = Field(default_factory=list)
    focus: str | None = None
    enableWebSearch: bool = True


class AiCitation(BaseModel):
    title: str
    url: str
    snippet: str
    source: str


class AiChatResponse(BaseModel):
    brandName: str
    provider: str
    model: str
    filtered: bool
    rulesVersion: str
    answer: str
    citations: list[AiCitation]
    generatedAt: int


class AiModelConfigResponse(BaseModel):
    brandName: str
    rulesVersion: str
    defaultProvider: str
    defaultModel: str
    providers: dict[str, list[str]]


class TimeNowResponse(BaseModel):
    timezone: str
    localDateTime: str
    utcDateTime: str
    epochSeconds: int
    epochMilliseconds: int
    dayOfWeek: str
    date: str


class MarketQuote(BaseModel):
    key: str
    symbol: str
    name: str
    price: float
    currency: str
    change: float
    changePercent: float
    marketTime: str
    marketState: str
    source: str


class MarketSnapshotResponse(BaseModel):
    provider: str
    timezone: str
    localDateTime: str
    utcDateTime: str
    generatedAt: int
    cached: bool
    quotes: list[MarketQuote]


class FuelPriceItem(BaseModel):
    product: str
    region1Price: float
    region2Price: float | None = None
    unit: str = "VND/liter"


class VietnamFuelPriceResponse(BaseModel):
    provider: str
    sourceUrl: str
    timezone: str
    localDateTime: str
    generatedAt: int
    cached: bool
    prices: list[FuelPriceItem]
